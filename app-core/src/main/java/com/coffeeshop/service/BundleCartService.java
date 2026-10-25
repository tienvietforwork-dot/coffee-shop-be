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
    private final StockService stockService;
    public record Component(long itemId, long coffeeId, String name, int quantity, boolean optional, BigDecimal price) {}
    public record Bundle(long id, String name, BigDecimal basePrice, List<Component> components) {}
    public record CartBundle(long id, long bundleId, String name, int quantity, List<Long> selected,
                             BigDecimal unitPrice, BigDecimal total) {}
    public record Purchase(Bundle bundle, int quantity, List<Long> selected, BigDecimal optionalUnitPrice, BigDecimal unitPrice,
                           List<PricingService.InputLine> lines) {}
    private Bundle active(long id) {
        List<Bundle> bs = jdbc.query("SELECT id,name,bundle_price FROM product_bundles WHERE id=? AND del_flag=false AND status='ACTIVE' AND (starts_at IS NULL OR starts_at<=CURRENT_TIMESTAMP) AND (ends_at IS NULL OR ends_at>=CURRENT_TIMESTAMP)",
                (rs,n)->new Bundle(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3),List.of()),id);
        if(bs.isEmpty()) throw new BadRequestException("Combo đã ngừng bán hoặc hết hạn");
        Bundle b=bs.get(0);
        List<Component> parts=jdbc.query("SELECT bi.id,bi.coffee_id,c.name,bi.quantity,bi.optional,c.price FROM product_bundle_items bi JOIN coffees c ON c.id=bi.coffee_id WHERE bi.bundle_id=? AND bi.del_flag=false AND c.del_flag=false AND c.status='AVAILABLE' AND c.auto_sold_out=false ORDER BY bi.display_order,bi.id",
                (rs,n)->new Component(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getInt(4),rs.getBoolean(5),rs.getBigDecimal(6)),id);
        Long all=jdbc.queryForObject("SELECT count(*) FROM product_bundle_items WHERE bundle_id=? AND del_flag=false",Long.class,id);
        if(parts.isEmpty() || all==null || all!=parts.size() || parts.stream().noneMatch(p->!p.optional()))
            throw new BadRequestException("Thành phần combo không còn khả dụng");
        return new Bundle(b.id(),b.name(),b.basePrice(),parts);
    }
    private static String encode(List<Long> ids) { return String.join(",", ids.stream().map(String::valueOf).toList()); }
    private static List<Long> decode(String s) { return s==null || s.isBlank()?List.of():Arrays.stream(s.split(",")).map(Long::valueOf).toList(); }
    public Purchase resolve(long bundleId, int qty, List<Long> optionalIds) {
        if(qty<1 || qty>99) throw new BadRequestException("Số lượng combo không hợp lệ");
        Bundle b=active(bundleId); Set<Long> chosen=new HashSet<>(optionalIds == null ? List.of():optionalIds);
        if(chosen.size() != (optionalIds==null?0:optionalIds.size()) || b.components().stream().filter(p->p.optional()).map(Component::itemId).filter(chosen::contains).count()!=chosen.size())
            throw new BadRequestException("Món tùy chọn không hợp lệ");
        List<PricingService.InputLine> lines=new ArrayList<>(); boolean priced=false; BigDecimal extra=BigDecimal.ZERO;
        for(Component p:b.components()) {
            if(p.optional() && !chosen.contains(p.itemId())) continue;
            Coffee coffee=coffeeService.get(p.coffeeId());
            for(int n=0;n<p.quantity()*qty;n++) {
                BigDecimal unit;
                if(p.optional()) {unit=p.price();extra=extra.add(unit);}
                else if(!priced) {unit=b.basePrice().multiply(BigDecimal.valueOf(qty));priced=true;}
                else unit=BigDecimal.ZERO;
                lines.add(new PricingService.InputLine(coffee,1,"Combo: "+b.name(),unit));
            }
        }
        stockService.requireStock(lines);
        return new Purchase(b,qty,optionalIds==null?List.of():optionalIds,extra.divide(BigDecimal.valueOf(qty)),b.basePrice().add(extra.divide(BigDecimal.valueOf(qty))),lines);
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
            return new CartBundle(id,bundleId,p.bundle().name(),qty,opts,p.unitPrice(),p.unitPrice().multiply(BigDecimal.valueOf(qty)));
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
}
