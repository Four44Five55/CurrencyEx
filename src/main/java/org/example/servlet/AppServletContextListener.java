package org.example.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.example.service.CentralBankService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener // Эта аннотация регистрирует слушатель
public class AppServletContextListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;
    private final CentralBankService cbrService = new CentralBankService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Web application is starting up...");

        // Создаем планировщик задач, который будет работать в фоновом потоке
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Задача, которую будем выполнять
        Runnable updateRatesTask = () -> {
            try {
                System.out.println("Executing scheduled task: Updating exchange rates...");
                cbrService.updateAllExchangeRates();
                System.out.println("Task finished: Exchange rates updated successfully.");
            } catch (Exception e) {
                System.err.println("Error during scheduled rate update: " + e.getMessage());
                e.printStackTrace(); // В реальном приложении здесь должен быть логгер
            }
        };

        // Запускаем задачу немедленно при старте,
        // а затем повторяем каждые 24 часа.
        // Вы можете настроить интервал как вам угодно.
        scheduler.scheduleAtFixedRate(
                updateRatesTask,
                0,            // начальная задержка (0 = запустить сразу)
                24,           // интервал
                TimeUnit.HOURS // единица измерения интервала
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Web application is shutting down...");
        // Обязательно останавливаем планировщик, когда приложение останавливается,
        // чтобы избежать утечек потоков.
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}