set @add_review_image_url = (
    select if(
        count(1) = 0,
        'alter table activity add column review_image_url varchar(500) null after image_url',
        'select 1'
    )
    from information_schema.columns
    where table_schema = database()
      and table_name = 'activity'
      and column_name = 'review_image_url'
);
prepare add_review_image_url_stmt from @add_review_image_url;
execute add_review_image_url_stmt;
deallocate prepare add_review_image_url_stmt;

set @add_review_content = (
    select if(
        count(1) = 0,
        'alter table activity add column review_content text null after review_image_url',
        'select 1'
    )
    from information_schema.columns
    where table_schema = database()
      and table_name = 'activity'
      and column_name = 'review_content'
);
prepare add_review_content_stmt from @add_review_content;
execute add_review_content_stmt;
deallocate prepare add_review_content_stmt;
