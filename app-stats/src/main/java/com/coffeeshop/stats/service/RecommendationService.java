package com.coffeeshop.stats.service;
import com.coffeeshop.stats.entity.*;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import com.coffeeshop.stats.entity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
@Service @RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRuleRepository rules;
    private final RecommendationEventRepository events;
    private final CoffeeRepository coffees;
    public record Input(String name,String type,Long anchorCoffeeId,Long targetCoffeeId,int priority) {}
    public record EventInput(Long ruleId,Long coffeeId,String impressionId,String eventType) {}
    private ResponseStatusException bad(String msg){return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg);}
    private RecommendationRule require(Long id){return rules.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy quy tắc"));}
    private Coffee coffee(Long id){return coffees.findById(id).orElseThrow(()->bad("Không tìm thấy sản phẩm: "+id));}
    private void validate(Input v){
        if(v==null||v.name()==null||v.name().isBlank()||v.name().length()>150)throw bad("Tên quy tắc 1-150 ký tự");
        if(!List.of("PRODUCT","CATEGORY","SIMILAR").contains(v.type()))throw bad("Loại quy tắc không hợp lệ");
        if(v.anchorCoffeeId()==null)throw bad("Chọn sản phẩm gốc");
        if(v.priority()<0||v.priority()>10000)throw bad("Độ ưu tiên 0-10000");
        coffee(v.anchorCoffeeId());
        if("PRODUCT".equals(v.type())){
            if(v.targetCoffeeId()==null||v.targetCoffeeId().equals(v.anchorCoffeeId()))throw bad("Chọn món gợi ý khác món gốc");
            coffee(v.targetCoffeeId());
        }
    }
    private void apply(RecommendationRule rule,Input v){
        rule.setName(v.name().trim());rule.setType(v.type());rule.setAnchorCoffee(coffee(v.anchorCoffeeId()));
        rule.setTargetCoffee("PRODUCT".equals(v.type())?coffee(v.targetCoffeeId()):null);
        rule.setPriority(v.priority());rule.setUpdatedAt(LocalDateTime.now());
    }
    private Map<String,Object> describe(RecommendationRule r){
        Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("name",r.getName());m.put("type",r.getType());
        m.put("anchorCoffeeId",r.getAnchorCoffee().getId());m.put("anchorName",r.getAnchorCoffee().getName());
        m.put("targetCoffeeId",r.getTargetCoffee()==null?null:r.getTargetCoffee().getId());
        m.put("targetName",r.getTargetCoffee()==null?null:r.getTargetCoffee().getName());
        m.put("priority",r.getPriority());m.put("status",r.getStatus());
        m.put("impressions",events.countByRuleIdAndEventType(r.getId(),"IMPRESSION"));
        m.put("clicks",events.countByRuleIdAndEventType(r.getId(),"CLICK"));
        m.put("addToCart",events.countByRuleIdAndEventType(r.getId(),"ADD_TO_CART"));
        return m;
    }
    @Transactional(readOnly=true) public List<Map<String,Object>> list(){return rules.findAllByOrderByPriorityDescIdDesc().stream().map(this::describe).toList();}
    @Transactional public Map<String,Object> create(Input v){validate(v);RecommendationRule r=new RecommendationRule();apply(r,v);return describe(rules.saveAndFlush(r));}
    @Transactional public Map<String,Object> update(Long id,Input v){RecommendationRule r=require(id);if("ACTIVE".equals(r.getStatus()))throw bad("Tạm dừng quy tắc trước khi sửa");validate(v);apply(r,v);return describe(rules.saveAndFlush(r));}
    @Transactional public Map<String,Object> status(Long id,String status){if(!List.of("ACTIVE","PAUSED").contains(status))throw bad("Trạng thái không hợp lệ");RecommendationRule r=require(id);r.setStatus(status);r.setUpdatedAt(LocalDateTime.now());return describe(rules.saveAndFlush(r));}
    @Transactional public void delete(Long id){RecommendationRule r=require(id);if("ACTIVE".equals(r.getStatus()))throw bad("Tạm dừng trước khi xóa");rules.delete(r);}
    private Map<String,Object> suggestion(RecommendationRule r,Coffee c){
        Map<String,Object> m=new LinkedHashMap<>();m.put("ruleId",r.getId());m.put("coffeeId",c.getId());m.put("coffeeName",c.getName());m.put("price",c.getPrice());
        m.put("imageUrl",c.imageSrc());m.put("description",c.getDescription());m.put("reason",switch(r.getType()){case "CATEGORY"->"Cùng danh mục";case "SIMILAR"->"Sản phẩm tương tự";default->"Được đề xuất";});m.put("priority",r.getPriority());return m;
    }
    @Transactional(readOnly=true) public List<Map<String,Object>> recommend(Long anchorId,int limit,boolean preview){
        if(anchorId==null)return List.of();Coffee anchor=coffee(anchorId);int cap=Math.max(1,Math.min(limit,12));
        List<Map<String,Object>> out=new ArrayList<>();Set<Long> seen=new HashSet<>();
        List<Coffee> catalog=coffees.findAllByOrderByNameAsc();
        List<RecommendationRule> source=preview?rules.findAllByOrderByPriorityDescIdDesc():rules.findByStatusOrderByPriorityDescIdDesc("ACTIVE");
        for(RecommendationRule r:source){
            if(!r.getAnchorCoffee().getId().equals(anchorId))continue;
            List<Coffee> candidates=new ArrayList<>();
            if("PRODUCT".equals(r.getType())){if(r.getTargetCoffee()!=null)candidates.add(r.getTargetCoffee());}
            else for(Coffee c:catalog){
                if(c.getId().equals(anchorId)||!c.getCategory().getId().equals(anchor.getCategory().getId()))continue;
                if("SIMILAR".equals(r.getType())){
                    double base=anchor.getPrice().doubleValue(); if(base<=0||Math.abs(c.getPrice().doubleValue()-base)/base>0.30)continue;
                }
                candidates.add(c);
            }
            for(Coffee c:candidates){
                if(c.getStatus()!=CoffeeStatus.AVAILABLE||c.isAutoSoldOut()||!seen.add(c.getId()))continue;
                out.add(suggestion(r,c));if(out.size()>=cap)return out;
            }
        }
        return out;
    }
    @Transactional public void event(EventInput input){
        if(input==null||input.ruleId()==null||input.coffeeId()==null||!List.of("IMPRESSION","CLICK","ADD_TO_CART").contains(input.eventType()))throw bad("Sự kiện không hợp lệ");
        UUID uuid;try{uuid=UUID.fromString(input.impressionId());}catch(Exception e){throw bad("impressionId không hợp lệ");}
        RecommendationRule r=require(input.ruleId());if(!"ACTIVE".equals(r.getStatus()))throw bad("Quy tắc chưa kích hoạt");
        // Verify the event's coffee belongs to this rule's currently eligible recommendations.
        boolean eligible=recommend(r.getAnchorCoffee().getId(),12,false).stream().anyMatch(x->x.get("ruleId").equals(r.getId())&&x.get("coffeeId").equals(input.coffeeId()));
        if(!eligible)throw bad("Sản phẩm không thuộc danh sách đề xuất");
        events.insertIfAbsent(r.getId(),input.coffeeId(),uuid,input.eventType());
    }
    @Transactional(readOnly=true) public Map<String,Object> analytics(Long id){RecommendationRule r=require(id);Map<String,Object> map=describe(r);long views=(long)map.get("impressions"),clicks=(long)map.get("clicks");map.put("clickThroughRate",views==0?0d:(double)clicks/views);map.put("purchasesTracked",false);map.put("note","Chưa đối soát giỏ hàng/đơn thành công; ADD_TO_CART là sự kiện lựa chọn, không phải xác nhận giao dịch.");return map;}
}
