-- Regra 1: confirmação de pedido por e-mail, somente entre 08h e 22h (horário de Brasília)
INSERT INTO rules (id, name, description, event_type, priority, active, condition_tree)
VALUES ('11111111-1111-1111-1111-111111111111',
        'Pedido criado - confirmação por e-mail',
        'Envia e-mail de confirmação para pedidos com valor maior que zero',
        'PEDIDO_CRIADO', 10, TRUE,
        '{"type":"AND","children":[{"type":"LEAF","field":"order.total","op":"GT","value":0}]}'::jsonb);

INSERT INTO rule_actions (rule_id, channel, template, recipient_expression)
VALUES ('11111111-1111-1111-1111-111111111111', 'EMAIL',
        'Olá {{user.name}}, recebemos seu pedido {{order.id}} no valor de R$ {{order.total}}.',
        'user.email');

INSERT INTO rule_constraints (rule_id, type, params)
VALUES ('11111111-1111-1111-1111-111111111111', 'RATE_LIMIT', '{"max":5,"windowSeconds":3600}'::jsonb),
       ('11111111-1111-1111-1111-111111111111', 'TIME_WINDOW',
        '{"start":"08:00","end":"22:00","zone":"America/Sao_Paulo"}'::jsonb);

-- Regra 2: falha de pagamento (2+ tentativas ou cartão de crédito) por SMS e push, com cooldown de 5 minutos
INSERT INTO rules (id, name, description, event_type, priority, active, condition_tree)
VALUES ('22222222-2222-2222-2222-222222222222',
        'Pagamento falhou - alerta por SMS e push',
        'Alerta o cliente quando o pagamento falha repetidamente ou no cartão de crédito',
        'PAGAMENTO_FALHOU', 5, TRUE,
        '{"type":"OR","children":[{"type":"LEAF","field":"payment.attempts","op":"GTE","value":2},{"type":"LEAF","field":"payment.method","op":"IN","value":["CREDIT_CARD"]}]}'::jsonb);

INSERT INTO rule_actions (rule_id, channel, template, recipient_expression)
VALUES ('22222222-2222-2222-2222-222222222222', 'SMS',
        'Seu pagamento do pedido {{order.id}} falhou. Tente novamente.', 'user.phone'),
       ('22222222-2222-2222-2222-222222222222', 'PUSH',
        'Pagamento recusado no pedido {{order.id}}.', 'user.deviceToken');

INSERT INTO rule_constraints (rule_id, type, params)
VALUES ('22222222-2222-2222-2222-222222222222', 'COOLDOWN', '{"seconds":300}'::jsonb);
