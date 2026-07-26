SET NOCOUNT ON;

IF DB_ID(N'StackOverflow2010') IS NULL
BEGIN
    CREATE DATABASE [StackOverflow2010]
    ON
        (FILENAME = N'/var/opt/mssql/data/StackOverflow2010.mdf'),
        (FILENAME = N'/var/opt/mssql/data/StackOverflow2010_log.ldf')
    FOR ATTACH;
END;
