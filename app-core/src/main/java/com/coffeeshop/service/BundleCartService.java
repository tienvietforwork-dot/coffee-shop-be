package com.coffeeshop.service;

import com.coffeeshop.entity.Cart;
import com.coffeeshop.entity.Coffee;
import com.coffeeshop.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Database is shared with app-stats. Re-check bundle price, schedule and components at checkout. */
@Service @RequiredArgsConstructor
public class BundleCartService {
    private final JdbcTemplate jdbc;
    private final CoffeeService coffeeService;
    public record Component(long itemId, long coffeeId, String name, int quantity, boolean optional,
                            String choiceGroup, BigDecimal price, boolean available, String categoryType) {}
    public record Bundle(long id, String name, String imageUrl, BigDecimal basePrice, List<Component> components) {}
    public record CartComponent(long coffeeId, String name, int quantity, boolean optional,
                                String choiceGroup, BigDecimal unitPrice) {}
    public record CartBundle(long id, long bundleId, String name, String imageUrl, int quantity, List<Long> selected,
                             List<CartComponent> components, BigDecimal unitPrice, BigDecimal total) {}
    public record Purchase(Bundle bundle, int quantity, List<Long> selected, BigDecimal optionalUnitPrice, BigDecimal unitPrice,
                           List<PricingService.InputLine> lines) {}
    private Bundle active(long id) {
        List<Bundle> bs = jdbc.query("SELECT id,name,image_id,image_url,bundle_price FROM product_bundles WHERE id=? AND del_flag=false AND status='ACTIVE' AND (starts_at IS NULL OR starts_at<=CURRENT_TIMESTAMP) AND (ends_at IS NULL OR ends_at>CURRENT_TIMESTAMP)",
                (rs,n)-> {
                    Long imageId = rs.getObject(3, Long.class);
                    String imageUrl = imageId != null ? "/api/public/images/" + imageId : rs.getString(4);
                    return new Bundle(rs.getLong(1),rs.getString(2),imageUrl,rs.getBigDecimal(5),List.of());
                },id);
        if(bs.isEmpty()) throw new BadRequestException("Combo đã ngừng bán hoặc hết hạn");
        Bundle b=bs.get(0);
        List<Component> parts=jdbc.query("SELECT bi.id,bi.coffee_id,c.name,bi.quantity,bi.optional,bi.choice_group,c.price,c.status,c.auto_sold_out,cat.product_type FROM product_bundle_items bi JOIN coffees c ON c.id=bi.coffee_id JOIN categories cat ON cat.id=c.category_id WHERE bi.bundle_id=? AND bi.del_flag=false AND c.del_flag=false ORDER BY bi.display_order,bi.id",
                (rs,n)->new Component(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getInt(4),rs.getBoolean(5),
                        rs.getString(6),rs.getBigDecimal(7),"AVAILABLE".equals(rs.getString(8))&&!rs.getBoolean(9),rs.getString(10)),id);
        if(parts.isEmpty() || parts.stream().noneMatch(p->!p.optional() && p.available()))
            throw new BadRequestException("Combo không còn món bắt buộc để bán");
        if (parts.stream().anyMatch(p -> !"DRINK".equals(p.categoryType())))
            throw new BadRequestException("Combo chỉ bao gồm cà phê/đồ uống; hãy cập nhật lại thành phần trước khi mua");
        if (parts.size() != 2 || parts.stream().anyMatch(p -> p.optional()
                || (p.choiceGroup() != null && !p.choiceGroup().isBlank()))
                || parts.stream().map(Component::coffeeId).distinct().count() != 2)
            throw new BadRequestException("Combo phải gồm đúng 2 loại cà phê khác nhau; hãy cập nhật thành phần trước khi mua");
        BigDecimal standalone = parts.stream()
                .map(p -> p.price().multiply(BigDecimal.valueOf(p.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (b.basePrice().compareTo(standalone) > 0)
            throw new BadRequestException("Giá sản phẩm đã thay đổi, combo đang cần được quản lý cập nhật trước khi bán");
        return new Bundle(b.id(),b.name(),b.imageUrl(),b.basePrice(),parts);
    }
    private static String encode(List<Long> ids) { return String.join(",", ids.stream().map(String::valueOf).toList()); }
    private static List<Long> decode(String s) { return s==null || s.isBlank()?List.of():Arrays.stream(s.split(",")).map(Long::valueOf).toList(); }
    public Purchase resolve(long bundleId, int qty, List<Long> optionalIds) {
        if(qty<1 || qty>99) throw new BadRequestException("Số lượng combo không hợp lệ");
        Bundle b=active(bundleId); List<Long> requested=optionalIds==null?List.of():optionalIds;
        if (!requested.isEmpty()) throw new BadRequestException("Combo gồm 2 cà phê cố định, không có món cần chọn thêm");
        List<Long> chosen=List.of();
        List<PricingService.InputLine> lines=new ArrayList<>();
        List<IncludedUnit> includedUnits=new ArrayList<>();
        for(Component p:b.components()) {
            if(!p.available()) throw new BadRequestException(p.name()+" trong combo đã hết hàng");
            Coffee coffee=coffeeService.get(p.coffeeId());
            String note="Combo: "+b.name()+(p.choiceGroup()==null||p.choiceGroup().isBlank()?"":" · "+p.choiceGroup());
            for(int n=0;n<p.quantity()*qty;n++) {
                includedUnits.add(new IncludedUnit(coffee,note,p.price()));
            }
        }

        // Allocate the fixed combo price over its included component units. This keeps invoice/order lines
        // readable and makes component revenue analytics reflect the actual bundle revenue distribution.
        BigDecimal bundleTotal=b.basePrice().multiply(BigDecimal.valueOf(qty));
        BigDecimal totalWeight=includedUnits.stream().map(IncludedUnit::weight).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal allocated=BigDecimal.ZERO;
        for (int i=0;i<includedUnits.size();i++) {
            IncludedUnit unit=includedUnits.get(i);
            BigDecimal price=i==includedUnits.size()-1
                    ? bundleTotal.subtract(allocated)
                    : bundleTotal.multiply(unit.weight()).divide(totalWeight,2,java.math.RoundingMode.DOWN);
            lines.add(new PricingService.InputLine(unit.coffee(),1,unit.note(),price));
            allocated=allocated.add(price);
        }
        return new Purchase(b,qty,List.copyOf(chosen),BigDecimal.ZERO,b.basePrice(),lines);
    }
    @Transactional public void add(Cart cart,long bundleId,int quantity,List<Long> options) {
        resolve(bundleId,quantity,options);
        jdbc.update("INSERT INTO cart_bundle_lines(cart_id,bundle_id,quantity,optional_item_ids) VALUES(?,?,?,?)",cart.getId(),bundleId,quantity,encode(options==null?List.of():options));
    }
    @Transactional public void remove(Cart cart,long lineId) {
        if(jdbc.update("DELETE FROM cart_bundle_lines WHERE id=? AND cart_id=?",lineId,cart.getId())==0) throw new BadRequestException("Không tìm thấy combo trong giỏ");
    }
    public List<CartBundle> display(Cart cart) {
        return jdbc.query("SELECT id,bundle_id,quantity,optional_item_ids FROM cart_bundle_lines WHERE cart_id=? ORDER BY id",(rs,n)->{
            long id=rs.getLong(1),bundleId=rs.getLong(2); int qty=rs.getInt(3);List<Long> opts=decode(rs.getString(4));
            Purchase p=resolve(bundleId,qty,opts);
            List<CartComponent> selectedComponents=p.bundle().components().stream()
                    .filter(c -> !c.optional() || p.selected().contains(c.itemId()))
                    .map(c -> new CartComponent(c.coffeeId(),c.name(),c.quantity(),c.optional(),c.choiceGroup(),c.price()))
                    .toList();
            return new CartBundle(id,bundleId,p.bundle().name(),p.bundle().imageUrl(),qty,p.selected(),selectedComponents,
                    p.unitPrice(),p.unitPrice().multiply(BigDecimal.valueOf(qty)));
        },cart.getId());
    }
    public List<Purchase> purchases(Cart cart) {
        return jdbc.query("SELECT bundle_id,quantity,optional_item_ids FROM cart_bundle_lines WHERE cart_id=? ORDER BY id",(rs,n)->resolve(rs.getLong(1),rs.getInt(2),decode(rs.getString(3))),cart.getId());
    }
    public List<PricingService.InputLine> lines(Cart cart) {
        return purchases(cart).stream().flatMap(p->p.lines().stream()).toList();
    }
    public void saveSales(long orderId,List<Purchase> purchases) {
        for(Purchase p:purchases) {
            String snapshot=p.lines().stream().map(l->l.coffee().getId()+"x"+l.quantity()).reduce((a,b)->a+","+b).orElse("");
            org.springframework.jdbc.support.GeneratedKeyHolder holder=new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbc.update(connection -> {
                var ps=connection.prepareStatement("INSERT INTO order_bundle_sales(order_id,bundle_id,bundle_name,quantity,unit_price,optional_total,component_snapshot) VALUES(?,?,?,?,?,?,?)",new String[]{"id"});
                ps.setLong(1,orderId); ps.setLong(2,p.bundle().id()); ps.setString(3,p.bundle().name());
                ps.setInt(4,p.quantity()); ps.setBigDecimal(5,p.bundle().basePrice());
                ps.setBigDecimal(6,p.optionalUnitPrice().multiply(BigDecimal.valueOf(p.quantity())));
                ps.setString(7,snapshot); return ps;
            },holder);
            long saleId=java.util.Objects.requireNonNull(holder.getKey()).longValue();
            for(var line:p.lines()) jdbc.update("INSERT INTO order_bundle_components(sale_id,coffee_id,quantity,line_total) VALUES(?,?,?,?)",
                    saleId,line.coffee().getId(),line.quantity(),line.fixedPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
    }
    private record IncludedUnit(Coffee coffee,String note,BigDecimal weight) {}
}
