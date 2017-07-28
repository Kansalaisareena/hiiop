alter table quests add constraint name_or_unmoderated_name check(name notnull or unmoderated_name notnull);
alter table quests add constraint description_or_unmoderated_description check(description notnull or unmoderated_description notnull);
