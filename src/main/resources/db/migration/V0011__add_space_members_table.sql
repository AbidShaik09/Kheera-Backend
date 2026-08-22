CREATE TABLE space_members (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                               user_id UUID NOT NULL,
                               space_id UUID NOT NULL,
                               space_role_id UUID NOT NULL,

                               CONSTRAINT fk_space_member_user
                                   FOREIGN KEY (user_id) REFERENCES users(id),

                               CONSTRAINT fk_space_member_space
                                   FOREIGN KEY (space_id) REFERENCES spaces(id),

                               CONSTRAINT fk_space_member_role
                                   FOREIGN KEY (space_role_id) REFERENCES space_roles(id),

                               CONSTRAINT uq_space_member
                                   UNIQUE (user_id, space_id)
);