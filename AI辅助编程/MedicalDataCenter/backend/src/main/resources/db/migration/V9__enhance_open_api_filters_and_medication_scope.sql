insert into api_scope (code, name, description)
select 'medications.read', 'Read medications', 'Query medication orders'
where not exists (
    select 1 from api_scope where code = 'medications.read'
);

update api_client
set scopes = concat(scopes, ',medications.read')
where client_id = 'demo-client'
  and scopes not like '%medications.read%';

insert into api_rate_limit_rule (client_id, endpoint, capacity, refill_tokens, refill_seconds)
select 'demo-client', '/open-api/v1/medications', 10, 10, 60
where not exists (
    select 1
    from api_rate_limit_rule
    where client_id = 'demo-client'
      and endpoint = '/open-api/v1/medications'
);
