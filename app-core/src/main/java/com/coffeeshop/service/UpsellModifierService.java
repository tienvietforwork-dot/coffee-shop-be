package com.coffeeshop.service;

import com.coffeeshop.dto.response.CartResponse;
import com.coffeeshop.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Server-side validation, pricing and material usage of ordered coffee modifiers. */
@Service @RequiredArgsConstructor
public class UpsellModifierService {
    private final NamedParameterJdbcTemplate jdbc;
    public record Option(Long id, Long coffeeId, String name, String kind, BigDecimal price, boolean enabled, boolean recipeReady) {}
    public record MaterialNeed(Long materialId, BigDecimal quantity) {}
    public static String serialize(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }
    public static List<Long> parse(String encoded) {
        if (encoded == null || encoded.isBlank()) return List.of();
        return Arrays.stream(encoded.split(",")).map(Long::parseLong).toList();
    }
    private Option map(Map<String,Object> row) {
        return new Option(((Number)row.get("id")).longValue(),((Number)row.get("coffee_id")).longValue(),
                (String)row.get("name"),(String)row.get("kind"),(BigDecimal)row.get("extra_price"),
                Boolean.TRUE.equals(row.get("enabled")), Boolean.TRUE.equals(row.get("recipe_ready")));
    }
    public List<Option> get(List<Long> ids) {
        if(ids == null || ids.isEmpty()) return List.of();
        if(ids.size()>12 || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size()!=ids.size())
            throw new BadRequestException("Danh sách nâng cấp không hợp lệ");
        var params=new MapSqlParameterSource("ids",ids);
        List<Option> found=jdbc.queryForList("SELECT id,coffee_id,name,kind,extra_price,enabled,recipe_ready FROM public.upsell_options WHERE id IN (:ids)",params)
                .stream().map(this::map).toList();
        if(found.size()!=ids.size()) throw new BadRequestException("Có tùy chọn nâng cấp không tồn tại");
        return found;
    }
    public List<Option> validated(Long coffeeId, List<Long> ids) {
        List<Option> found=get(ids);
        long premiums=found.stream().filter(o->"PREMIUM".equals(o.kind())).count();
        if(premiums>1) throw new BadRequestException("Chỉ chọn một phiên bản thành phần cao cấp");
        for(Option o:found) {
            if("SIZE".equals(o.kind()) || !o.coffeeId().equals(coffeeId)||!o.enabled()||!o.recipeReady())
                throw new BadRequestException("Tùy chọn " + o.name() + " chưa sẵn sàng cho món này");
            Integer count=jdbc.queryForObject("SELECT count(*) FROM public.upsell_option_materials WHERE option_id=:id",
                    new MapSqlParameterSource("id",o.id()),Integer.class);
            if(count==null||count==0) throw new BadRequestException("Tùy chọn " + o.name() + " chưa có định lượng nguyên liệu");
        }
        return found;
    }
    public BigDecimal surcharge(Long coffeeId, String encoded) {
        return validated(coffeeId,parse(encoded)).stream().map(Option::price).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    public List<CartResponse.OptionLine> labels(String encoded) {
        return get(parse(encoded)).stream().map(o->new CartResponse.OptionLine(o.id(),o.name(),o.kind(),o.price())).toList();
    }
    public List<MaterialNeed> materials(String encoded) {
        List<Long> ids=parse(encoded);
        if(ids.isEmpty())return List.of();
        return jdbc.query("SELECT material_id, sum(quantity) AS quantity FROM public.upsell_option_materials WHERE option_id IN (:ids) GROUP BY material_id",
                new MapSqlParameterSource("ids",ids),(rs,n)->new MaterialNeed(rs.getLong("material_id"),rs.getBigDecimal("quantity")));
    }
    public List<Map<String,Object>> publicOptions(Long coffeeId) {
        return jdbc.queryForList("""
          SELECT o.id,o.coffee_id AS "coffeeId",o.kind,o.name,o.extra_price AS "extraPrice",o.extra_volume_ml AS "extraVolumeMl"
          FROM public.upsell_options o WHERE o.coffee_id=:id AND o.kind <> 'SIZE' AND o.enabled=true AND o.recipe_ready=true
            AND EXISTS (SELECT 1 FROM public.upsell_option_materials m WHERE m.option_id=o.id)
          ORDER BY o.kind,o.id
          """,new MapSqlParameterSource("id",coffeeId));
    }
}
