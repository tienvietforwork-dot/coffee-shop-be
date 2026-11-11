package com.coffeeshop.stats.service;

import com.coffeeshop.stats.entity.*;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import com.coffeeshop.stats.entity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class MerchandisingScheduleService {
    private final MerchandisingScheduleRepository schedules;
    private final CoffeeRepository coffees;
    private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> SECTIONS = Set.of("HOME","FEATURED","BEST_SELLER","NEW_ARRIVAL","RECOMMENDED");
    private static final Set<String> SLOTS = Set.of("MORNING","AFTERNOON","WEEKEND","SEASONAL","CUSTOM");
    public record Input(String name,Long coffeeId,String section,String slot,Integer priority,LocalDateTime startsAt,LocalDateTime endsAt,List<Integer> weekdays,List<Integer> months) {}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private MerchandisingSchedule require(long id){return schedules.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy lịch"));}
    private static Set<Integer> decode(String value){Set<Integer> result=new TreeSet<>();if(value!=null&&!value.isBlank())for(String s:value.split(","))result.add(Integer.parseInt(s));return result;}
    private static String encode(Collection<Integer> values){return values==null||values.isEmpty()?null:String.join(",",new TreeSet<>(values).stream().map(String::valueOf).toList());}
    private Set<Integer> weekdays(MerchandisingSchedule x){Set<Integer> explicit=decode(x.getWeekdays());if(!explicit.isEmpty())return explicit;return "WEEKEND".equals(x.getSlot())?Set.of(6,7):Set.of(1,2,3,4,5,6,7);}
    private Set<Integer> months(MerchandisingSchedule x){Set<Integer> m=decode(x.getMonths());return m.isEmpty()?Set.of(1,2,3,4,5,6,7,8,9,10,11,12):m;}
    private boolean matches(MerchandisingSchedule x,LocalDateTime at){
        if(!"PUBLISHED".equals(x.getStatus())||at.isBefore(x.getStartsAt())||!at.isBefore(x.getEndsAt()))return false;
        if(!weekdays(x).contains(at.getDayOfWeek().getValue())||!months(x).contains(at.getMonthValue()))return false;
        LocalTime time=at.toLocalTime();
        if("MORNING".equals(x.getSlot()))return !time.isBefore(LocalTime.of(5,0))&&time.isBefore(LocalTime.NOON);
        if("AFTERNOON".equals(x.getSlot()))return !time.isBefore(LocalTime.NOON)&&time.isBefore(LocalTime.of(18,0));
        return true;
    }
    private void validate(Input x){
        if(x==null||x.coffeeId()==null||x.name()==null||x.name().isBlank()||x.name().length()>150)throw bad("Tên lịch và sản phẩm bắt buộc, tên tối đa 150 ký tự");
        if(x.section()==null||x.slot()==null||!SECTIONS.contains(x.section())||!SLOTS.contains(x.slot()))throw bad("Khu vực hoặc loại lịch không hợp lệ");
        if(x.startsAt()==null||x.endsAt()==null||!x.startsAt().isBefore(x.endsAt()))throw bad("Ngày bắt đầu phải nhỏ hơn ngày kết thúc");
        if(x.priority()!=null&&(x.priority()<0||x.priority()>10000))throw bad("Ưu tiên từ 0 đến 10000");
        if(x.weekdays()!=null&&x.weekdays().stream().anyMatch(v->v==null||v<1||v>7))throw bad("Ngày trong tuần phải từ 1 đến 7");
        if(x.months()!=null&&x.months().stream().anyMatch(v->v==null||v<1||v>12))throw bad("Tháng phải từ 1 đến 12");
        if("WEEKEND".equals(x.slot())&&x.weekdays()!=null&&!x.weekdays().isEmpty()&&!Set.of(6,7).containsAll(x.weekdays()))throw bad("Lịch cuối tuần chỉ cho phép thứ Bảy và Chủ Nhật");
    }

    private void apply(MerchandisingSchedule r, Input x) {
        validate(x);
        r.setCoffee(
                coffees.findById(x.coffeeId())
                        .orElseThrow(() -> bad("Sản phẩm không tồn tại"))
        );
    }

    private boolean intersects(Set<Integer> x,Set<Integer> y){return !Collections.disjoint(x,y);}
    private void checkConflict(MerchandisingSchedule r){
        for(MerchandisingSchedule other:schedules.findAll()){
            if(Objects.equals(other.getId(),r.getId())||!"PUBLISHED".equals(other.getStatus()))continue;
            if(!other.getCoffee().getId().equals(r.getCoffee().getId())||!other.getSection().equals(r.getSection()))continue;
            if(!other.getStartsAt().isBefore(r.getEndsAt())||!r.getStartsAt().isBefore(other.getEndsAt()))continue;
            if(!intersects(weekdays(other),weekdays(r))||!intersects(months(other),months(r)))continue;
            if(("MORNING".equals(r.getSlot())&&"AFTERNOON".equals(other.getSlot()))||("AFTERNOON".equals(r.getSlot())&&"MORNING".equals(other.getSlot())))continue;
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Lịch chồng với lịch #"+other.getId()+" (cùng sản phẩm và khu vực)");
        }
    }
    private Map<String,Object> output(MerchandisingSchedule r,LocalDateTime now){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("name",r.getName());m.put("coffeeId",r.getCoffee().getId());m.put("coffeeName",r.getCoffee().getName());m.put("price",r.getCoffee().getPrice());m.put("imageUrl",r.getCoffee().imageSrc());m.put("section",r.getSection());m.put("slot",r.getSlot());m.put("priority",r.getPriority());m.put("startsAt",r.getStartsAt());m.put("endsAt",r.getEndsAt());m.put("weekdays",decode(r.getWeekdays()));m.put("months",decode(r.getMonths()));m.put("status",r.getStatus());m.put("visibleNow",matches(r,now));return m;}
    private LocalDateTime now(){return LocalDateTime.now(SHOP_ZONE);}
    @Transactional(readOnly=true) public List<Map<String,Object>> list(){LocalDateTime t=now();return schedules.findAllByOrderByPriorityDescIdDesc().stream().map(r->output(r,t)).toList();}
    @Transactional public Map<String,Object> create(Input x){MerchandisingSchedule r=new MerchandisingSchedule();apply(r,x);return output(schedules.saveAndFlush(r),now());}
    @Transactional public Map<String,Object> update(long id,Input x){MerchandisingSchedule r=require(id);if("PUBLISHED".equals(r.getStatus()))throw bad("Thu hồi lịch trước khi chỉnh sửa");apply(r,x);return output(schedules.saveAndFlush(r),now());}
    @Transactional public Map<String,Object> publish(long id){MerchandisingSchedule r=require(id);if(!r.getEndsAt().isAfter(now()))throw bad("Không thể công bố lịch hết hạn");if(r.getCoffee().getStatus()!=CoffeeStatus.AVAILABLE||r.getCoffee().isAutoSoldOut())throw bad("Sản phẩm không sẵn bán");checkConflict(r);r.setStatus("PUBLISHED");return output(schedules.saveAndFlush(r),now());}
    @Transactional public Map<String,Object> unpublish(long id){MerchandisingSchedule r=require(id);r.setStatus("DRAFT");return output(schedules.saveAndFlush(r),now());}
    @Transactional public void delete(long id){MerchandisingSchedule r=require(id);if("PUBLISHED".equals(r.getStatus()))throw bad("Thu hồi lịch trước khi xóa");schedules.delete(r);}
    @Transactional(readOnly=true) public List<Map<String,Object>> preview(long id){MerchandisingSchedule r=require(id);return List.of(output(r,now()));}
    @Transactional(readOnly=true) public List<Map<String,Object>> publicNow(){LocalDateTime t=now();Set<String> seen=new HashSet<>();List<Map<String,Object>> result=new ArrayList<>();for(MerchandisingSchedule r:schedules.findAllByOrderByPriorityDescIdDesc()){Coffee c=r.getCoffee();String key=r.getSection()+":"+c.getId();if(matches(r,t)&&c.getStatus()==CoffeeStatus.AVAILABLE&&!c.isAutoSoldOut()&&seen.add(key))result.add(output(r,t));}return result;}
}
