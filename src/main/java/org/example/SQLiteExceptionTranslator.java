package org.example;

import org.example.exception.DataAccessException;
import org.example.exception.DataAccessResourceFailureException;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.sql.SQLException;

public final class SQLiteExceptionTranslator {
    private SQLiteExceptionTranslator() {
    }

    public static boolean isUniqueConstraintError(SQLException e) {
        return e instanceof SQLiteException &&
                e.getErrorCode() == SQLiteErrorCode.SQLITE_CONSTRAINT.code &&
                e.getMessage().contains("UNIQUE");
    }

    public static boolean isForeignKeyConstraintError(SQLException e) {
        return e instanceof SQLiteException &&
                e.getErrorCode() == SQLiteErrorCode.SQLITE_CONSTRAINT.code &&
                e.getMessage().contains("FOREIGN KEY");
    }

    public static DataAccessException translateToGeneralError(String task, SQLException e) {
        if (e instanceof SQLiteException) {
            int errorCode = ((SQLiteException) e).getErrorCode();

            // Проверяем на ошибки доступа к ресурсу
            if (errorCode == SQLiteErrorCode.SQLITE_CANTOPEN.code ||
                    errorCode == SQLiteErrorCode.SQLITE_IOERR.code) {
                return new DataAccessResourceFailureException("Не удалось выполнить " + task + ". Ошибка ресурса базы данных.", e);
            }
        }

        // Если не смогли распознать, бросаем общее исключение
        return new DataAccessException("Не удалось выполнить " + task + ". Ошибка ресурса базы данных.", e);
    }
}
