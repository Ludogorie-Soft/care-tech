-- Fix audit column name: BaseEntity uses last_modified_by, not updated_by
ALTER TABLE personal_offers RENAME COLUMN updated_by TO last_modified_by;
