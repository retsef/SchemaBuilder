# SchemaBuilder
Android Sqlite schema migration tool

I have difficulties with the migration in my projects for sqlite.
Sqlite dont have a propper "ALTER TABLE" syntax and can only append new column or rename tables.

So when i want to do migrations i need to write ugly and messy raw sql for the swapping meccanism of sqlite alter table.
To change schema in sqlite you must:
1. Create a new table with the new schema
2. Copy the data from the old table to the new
3. Delete the old table
4. Rename the new table with the name of the old table

With this class you can simplify this process.
It use the SupportSQLiteDatabase class to gather all the info about columns definiton, primary keys, relationships to determinate the schema.

# Examples
### Remove Column
I want to remove the column "note" from the table "events" so i will do:

```kotlin
val table_name = "events"
val schema = SchemaBuilder(db, table_name)

schema.columns.also {
    val subject = it.find { column -> column.name == "note" }
    it.remove(subject)
}

db.execSQL("create table `${table_name}_backup` ${schema.generateSchemaInfo()}")
db.execSQL("insert into `${table_name}_backup`(${schema.generateSchemaColumns()}) select ${schema.generateSchemaColumns()} from `$table_name`")
db.execSQL("drop table `$table_name`")
db.execSQL("alter table `${table_name}_backup` rename to `$table_name`")
```

### Add a new column before another
I want to add the column "nickname" as text after the column "surname" of the table "peoples", so i will do:

```kotlin
val table_name = "peoples"
val schema = SchemaBuilder(db, table_name)

schema.columns.also {
    val index = it.indexOfFirst { column -> column.name == "surname" }
    it.add(index+1, SchemaBuilder.Column("nickname", "TEXT"))
}

db.execSQL("create table `${table_name}_backup` ${schema.generateSchemaInfo()}")
db.execSQL("insert into `${table_name}_backup`(${schema.generateSchemaColumns()}) select ${schema.generateSchemaColumns()} from `$table_name`")
db.execSQL("drop table `$table_name`")
db.execSQL("alter table `${table_name}_backup` rename to `$table_name`")
```

### Rename column
This one is a bit trikie but my code `can do it` [cit]

I want to rename the column "second_name" to "family_name" of the table "peoples".
In this case i need the old schema and the new, so i will do:


```kotlin
val table_name = "peoples"
val old_schema = SchemaBuilder(db, table_name)
val schema = SchemaBuilder(db, table_name)

schema.columns.also {
    val subject = it.find { column -> column.name == "second_name" }
    subject.name = "family_name"
}

db.execSQL("create table `${table_name}_backup` ${schema.generateSchemaInfo()}")
db.execSQL("insert into `${table_name}_backup`(${schema.generateSchemaColumns()}) select ${old_schema.generateSchemaColumns()} from `$table_name`")
db.execSQL("drop table `$table_name`")
db.execSQL("alter table `${table_name}_backup` rename to `$table_name`")
```

