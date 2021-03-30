UPDATE user_details SET approved=false where approved IS NULL;

ALTER table user_details ALTER column approved set NOT NULL;