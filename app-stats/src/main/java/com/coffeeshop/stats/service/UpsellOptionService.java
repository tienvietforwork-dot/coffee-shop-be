package com.coffeeshop.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Administrative catalog only. Checkout integration must validate and price options in app-core. */
@Service
@RequiredArgsConstructor
public class UpsellOptionService {
    private final NamedParameterJdbcTemplate jdbc;
    public record OptionInput(Long coffeeId, String kind, String name, BigDecimal extraPrice,
                              Integer extraVolumeMl, Boolean enabled) {}

    private void validate(OptionInput x) {
        if (x == null || x.coffeeId() == null || x.kind() == null ||
                !Set.of("TOPPING", "PREMIUM").contains(x.kind()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn món và kiểu nâng cấp hợp lệ");
        if (x.name() == null || x.name().isBlank() || x.name().length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tùy chọn từ 1 đến 100 ký tự");
        if (x.extraPrice() == null || x.extraPrice().signum() < 0 || x.extraPrice().scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phụ phí không hợp lệ");
        if (x.extraVolumeMl() != null && (x.extraVolumeMl() < 0 || x.extraVolumeMl() > 2000))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dung tích thêm không hợp lệ");
        Integer exists = jdbc.queryForObject(
                "SELECT count(*) FROM public.coffees WHERE id=:id AND del_flag=false",
                new MapSqlParameterSource("id", x.coffeeId()), Integer.class);
        if (exists == null || exists == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Món gốc không tồn tại");
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> list(Long coffeeId) {
        String sql = """
          SELECT o.id, o.coffee_id AS "coffeeId", c.name AS "coffeeName",
                 o.kind, o.name, o.extra_price AS "extraPrice",
                 o.extra_volume_ml AS "extraVolumeMl", o.enabled,
                 o.recipe_ready AS "recipeReady"
          FROM public.upsell_options o JOIN public.coffees c ON c.id=o.coffee_id
          WHERE o.kind <> 'SIZE'
          """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (coffeeId != null) {
            sql += " AND o.coffee_id = :coffeeId";
            params.addValue("coffeeId", coffeeId);
        }
        sql += " ORDER BY c.name, o.kind, o.id";
        return jdbc.queryForList(sql, params);
    }

    @Transactional
    public void create(OptionInput x) {
        validate(x);
        jdbc.update("""
          INSERT INTO public.upsell_options
          (coffee_id,kind,name,extra_price,extra_volume_ml,enabled,recipe_ready)
          VALUES (:coffeeId,:kind,:name,:extraPrice,:extraVolumeMl,:enabled,false)
          """,new MapSqlParameterSource()
                .addValue("coffeeId",x.coffeeId()).addValue("kind",x.kind())
                .addValue("name",x.name().trim()).addValue("extraPrice",x.extraPrice())
                .addValue("extraVolumeMl",x.extraVolumeMl())
                .addValue("enabled",Boolean.TRUE.equals(x.enabled())));
    }

    @Transactional
    public void update(long id, OptionInput x) {
        validate(x);
        int count=jdbc.update("""
          UPDATE public.upsell_options SET coffee_id=:coffeeId,kind=:kind,
          name=:name,extra_price=:extraPrice,extra_volume_ml=:extraVolumeMl,
          enabled=:enabled,recipe_ready=false,updated_at=now() WHERE id=:id
          """,new MapSqlParameterSource("id",id).addValue("coffeeId",x.coffeeId())
                .addValue("kind",x.kind()).addValue("name",x.name().trim())
                .addValue("extraPrice",x.extraPrice()).addValue("extraVolumeMl",x.extraVolumeMl())
                .addValue("enabled",Boolean.TRUE.equals(x.enabled())));
        if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tùy chọn");
    }

    public record Ingredient(Long materialId, BigDecimal quantity) {}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> ingredients(long id) {
        return jdbc.queryForList("""
          SELECT m.material_id AS "materialId", i.name AS "materialName", i.unit, m.quantity
          FROM public.upsell_option_materials m JOIN public.materials i ON i.id=m.material_id
          WHERE m.option_id=:id ORDER BY i.name
          """, new MapSqlParameterSource("id",id));
    }
    @Transactional
    public void setIngredients(long id, List<Ingredient> ingredients) {
        Integer exists=jdbc.queryForObject("SELECT count(*) FROM public.upsell_options WHERE id=:id",new MapSqlParameterSource("id",id),Integer.class);
        if(exists==null||exists==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tùy chọn");
        if(ingredients==null||ingredients.isEmpty()||ingredients.size()>20)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cần ít nhất một nguyên liệu và tối đa 20 nguyên liệu");
        Set<Long> distinct=new java.util.HashSet<>();
        for(Ingredient ingredient:ingredients) {
            if(ingredient==null||ingredient.materialId()==null||ingredient.quantity()==null||ingredient.quantity().signum()<=0||ingredient.quantity().scale()>2||!distinct.add(ingredient.materialId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nguyên liệu và định lượng không hợp lệ hoặc bị trùng");
            Integer count=jdbc.queryForObject("SELECT count(*) FROM public.materials WHERE id=:id AND del_flag=false",
                    new MapSqlParameterSource("id",ingredient.materialId()),Integer.class);
            if(count==null||count==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nguyên liệu không tồn tại");
        }
        jdbc.update("UPDATE public.upsell_options SET recipe_ready=false,updated_at=now() WHERE id=:id",new MapSqlParameterSource("id",id));
        jdbc.update("DELETE FROM public.upsell_option_materials WHERE option_id=:id",new MapSqlParameterSource("id",id));
        for(Ingredient x:ingredients) jdbc.update("""
            INSERT INTO public.upsell_option_materials(option_id,material_id,quantity) VALUES (:id,:materialId,:quantity)
            """,new MapSqlParameterSource("id",id).addValue("materialId",x.materialId()).addValue("quantity",x.quantity()));
        jdbc.update("UPDATE public.upsell_options SET recipe_ready=true,updated_at=now() WHERE id=:id",new MapSqlParameterSource("id",id));
    }
    /**
     * Permanently remove only an option that has never been used in a cart or order.
     * A used option must remain for historical pricing/receipt reconstruction.
     */
    @Transactional
    public void delete(long id) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", id);
        List<Long> found = jdbc.queryForList(
                "SELECT id FROM public.upsell_options WHERE id=:id FOR UPDATE", p, Long.class);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tùy chọn");
        }

        String usedSql = """
                SELECT EXISTS (
                    SELECT 1 FROM public.cart_items ci
                    WHERE CAST(:id AS TEXT) = ANY(string_to_array(replace(coalesce(ci.option_ids, ''), ' ', ''), ','))
                ) OR EXISTS (
                    SELECT 1 FROM public.order_items oi
                    WHERE CAST(:id AS TEXT) = ANY(string_to_array(replace(coalesce(oi.option_ids, ''), ' ', ''), ','))
                )
                """;
        Boolean used = jdbc.queryForObject(usedSql, p, Boolean.class);
        if (Boolean.TRUE.equals(used)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tùy chọn đã được dùng trong giỏ hàng/đơn hàng. Hãy Tắt thay vì Xóa để giữ lịch sử.");
        }

        jdbc.update("DELETE FROM public.upsell_option_materials WHERE option_id=:id", p);
        jdbc.update("DELETE FROM public.upsell_options WHERE id=:id", p);
    }

    @Transactional
    public void disable(long id) {
        int count=jdbc.update("UPDATE public.upsell_options SET enabled=false,updated_at=now() WHERE id=:id",
                new MapSqlParameterSource("id",id));
        if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tùy chọn");
    }
}
