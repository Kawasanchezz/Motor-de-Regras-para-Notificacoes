CREATE TABLE rules (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(150) NOT NULL,
    description    TEXT,
    event_type     VARCHAR(100) NOT NULL,
    priority       INT          NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    condition_tree JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_rules_event_active_priority ON rules (event_type, active, priority);

CREATE TABLE rule_actions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id              UUID         NOT NULL REFERENCES rules (id) ON DELETE CASCADE,
    channel              VARCHAR(20)  NOT NULL,
    template             TEXT         NOT NULL,
    recipient_expression VARCHAR(255) NOT NULL
);

CREATE INDEX idx_rule_actions_rule ON rule_actions (rule_id);

CREATE TABLE rule_constraints (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID        NOT NULL REFERENCES rules (id) ON DELETE CASCADE,
    type    VARCHAR(30) NOT NULL,
    params  JSONB       NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_rule_constraints_rule ON rule_constraints (rule_id);

CREATE TABLE user_preferences (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    VARCHAR(100) NOT NULL,
    channel    VARCHAR(20)  NOT NULL,
    event_type VARCHAR(100),
    opted_out  BOOLEAN      NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- event_type NULL significa "todos os tipos de evento"
CREATE UNIQUE INDEX ux_user_preferences ON user_preferences (user_id, channel, COALESCE(event_type, ''));

CREATE TABLE decision_audit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(150) NOT NULL,
    idempotency_key VARCHAR(150),
    event_type      VARCHAR(100) NOT NULL,
    user_id         VARCHAR(100) NOT NULL,
    rule_id         UUID,
    decision        VARCHAR(10)  NOT NULL,
    reason          VARCHAR(200) NOT NULL,
    channel         VARCHAR(20),
    payload         JSONB,
    evaluated_rules JSONB,
    scheduled_for   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- sem FK para rule_id: o histórico deve sobreviver à exclusão da regra
CREATE INDEX idx_audit_user ON decision_audit (user_id);
CREATE INDEX idx_audit_event_type ON decision_audit (event_type);
CREATE INDEX idx_audit_decision ON decision_audit (decision);
CREATE INDEX idx_audit_created_at ON decision_audit (created_at DESC);
