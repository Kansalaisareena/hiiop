with cats as (
     select u.id as
     userid,
     json_array_elements(categories)::TEXT as catval
     from quests q, users u, parties p
     where p.user_id = u.id and
           q.id = p.quest_id),

     culture as (
     select userid, count(catval) > 0 as culture from cats
     where catval = to_json('culture'::TEXT)::TEXT
     group by cats.userid
     ),

     disabilities as (
     select userid, count(catval) > 0 as disabilities from cats
     where catval = to_json('disabilities'::TEXT)::TEXT
     group by cats.userid
     ),

     elderly as (
     select userid, count(catval) > 0 as elderly from cats
     where catval = to_json('elderly'::TEXT)::TEXT
     group by cats.userid
     ),

     environment as (
     select userid, count(catval) > 0 as environment from cats
     where catval = to_json('environment'::TEXT)::TEXT
     group by cats.userid
     ),

     equality as (
     select userid, count(catval) > 0 as equality from cats
     where catval = to_json('equality'::TEXT)::TEXT
     group by cats.userid
     ),

     foreignaid as (
     select userid, count(catval) > 0 as foreignaid from cats
     where catval = to_json('foreign-aid'::TEXT)::TEXT
     group by cats.userid
     ),

     inequality as (
     select userid, count(catval) > 0 as inequality from cats
     where catval = to_json('inequality'::TEXT)::TEXT
     group by cats.userid
     ),

     kidsandyoungsters as (
     select userid, count(catval) > 0 as kidsandyoungsters from cats
     where catval = to_json('kids-and-youngsters'::TEXT)::TEXT
     group by cats.userid
     ),

     peersupport as (
     select userid, count(catval) > 0 as peersupport from cats
     where catval = to_json('peer-support'::TEXT)::TEXT
     group by cats.userid
     ),

     wellbeing as (
     select userid, count(catval) > 0 as wellbeing from cats
     where catval = to_json('well-being'::TEXT)::TEXT
     group by cats.userid
     )

select users.email, culture as Kulttuurijataide, disabilities as Vammaisjamielenterveystyö, elderly as Ikäihmiset, environment as Ympäristöjaeläimet, equality as Monikulttuurisuusjatasavertaisuus, foreignaid as Kehitysjakatastrofiapu, inequality as Syrjäytyminenjaköyhyys, kidsandyoungsters as Lapsetjanuoret, peersupport as Vertaistukijaneuvonta, wellbeing as Liikuntajahyvinvointi
from users
left outer join culture on (users.id = culture.userid)
left outer join disabilities on (users.id = disabilities.userid)
left outer join elderly on (users.id = elderly.userid)           
left outer join environment on (users.id = environment.userid)
left outer join equality on (users.id = equality.userid)
left outer join foreignaid on (users.id = foreignaid.userid)
left outer join inequality on (users.id = inequality.userid)
left outer join kidsandyoungsters on (users.id = kidsandyoungsters.userid)
left outer join peersupport on (users.id = peersupport.userid)
left outer join wellbeing on (users.id = wellbeing.userid);














