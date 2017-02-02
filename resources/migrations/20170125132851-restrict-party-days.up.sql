UPDATE parties
SET days = GREATEST(days, -days);

--;;

ALTER TABLE parties
  ADD CONSTRAINT days_at_least_1 check (days > 0);

