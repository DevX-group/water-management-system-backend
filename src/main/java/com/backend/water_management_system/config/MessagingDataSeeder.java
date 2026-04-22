package com.backend.water_management_system.config;

import com.backend.water_management_system.dto.ScheduledMessageDto;
import com.backend.water_management_system.dto.ScheduledMessageDto.*;
import com.backend.water_management_system.service.ScheduledMessageService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Seeds the database with the initial scheduled messages (migrated from
 * frontend mock data)
 * if no messages exist yet.
 */
@Component
public class MessagingDataSeeder implements ApplicationRunner {

    private final ScheduledMessageService service;

    public MessagingDataSeeder(ScheduledMessageService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (service.count() > 0)
            return; // already seeded

        service.saveAll(buildSeedData());
    }

    private List<ScheduledMessageDto> buildSeedData() {
        return Arrays.asList(
                buildMonthlyBillMessage(),
                buildOverdueAlert(),
                buildWaterCutoff(),
                buildCustomMessageA());
    }

    private ScheduledMessageDto buildMonthlyBillMessage() {
        ScheduledMessageDto dto = new ScheduledMessageDto();
        dto.setName("Monthly Bill Message");
        dto.setChannels(Arrays.asList("SMS", "Email"));
        dto.setIsDefault(true);
        dto.setRecipients("All Customers");

        ScheduleDto schedule = new ScheduleDto();
        schedule.setType("Recurring");
        schedule.setDayOfMonth(20);
        schedule.setTime(LocalTime.of(8, 0));
        dto.setSchedule(schedule);

        MessageTemplateDto sms = buildDefaultBillTemplate();
        MessageTemplateDto email = buildDefaultBillTemplate();

        TemplatesDto templates = new TemplatesDto();
        templates.setSms(sms);
        templates.setEmail(email);
        dto.setTemplates(templates);

        return dto;
    }

    private MessageTemplateDto buildDefaultBillTemplate() {
        MessageTemplateDto t = new MessageTemplateDto();
        t.setIsCustom(false);
        t.setContent("");
        List<TemplateSectionDto> sections = Arrays.asList(
                section("1", "Greeting", "Dear {customer_name},"),
                section("2", "Introduction", "This is your monthly water bill notification from Pradeshiya Sabha."),
                section("3", "Customer Number Line", "Customer Number : {customer_number}"),
                section("4", "Billing Period Line", "Billing Period : {billing_period}"),
                section("5", "Bill Date Line", "Bill Date : {bill_date}"),
                section("6", "Base Charge Line", "Base Charge : LKR {base_charge}"),
                section("7", "Usage Units Line", "Units Used : {usage_units}"),
                section("8", "Usage Charge Line", "Charge For Usage : LKR {usage_charge}"),
                section("9", "Tax Amount Line", "Tax Amount : LKR {tax_amount}"),
                section("10", "Monthly Fee Line", "Monthly Fee : LKR {monthly_fee}"),
                section("11", "Outstanding Balance Line", "Outstanding Balance : LKR {outstanding_balance}"),
                section("12", "Total Balance Line", "Total Balance : LKR {total_balance}"),
                section("13", "Bill Link", "View your detailed bill online : {online_bill_portal_link}"),
                section("14", "Online Payment Instructions",
                        "For online payment, please visit www.example.com\nor\nDeposit the amount to account number {pradeshiya_sabha_acc_no} and Whatsapp your receipt along with your subscription number, name and NIC to {whatsApp_number}"),
                section("15", "Footer", "Thank you for your cooperation.\n- Pradeshiya Sabha"));
        t.setSections(sections);
        return t;
    }

    private ScheduledMessageDto buildOverdueAlert() {
        ScheduledMessageDto dto = new ScheduledMessageDto();
        dto.setName("Overdue Alert");
        dto.setChannels(Arrays.asList("SMS"));
        dto.setIsDefault(true);
        dto.setRecipients("Overdue Customers");

        ScheduleDto schedule = new ScheduleDto();
        schedule.setType("Recurring");
        schedule.setDayOfMonth(25);
        schedule.setTime(LocalTime.of(9, 0));
        dto.setSchedule(schedule);

        MessageTemplateDto sms = new MessageTemplateDto();
        sms.setIsCustom(false);
        sms.setContent("");
        sms.setSections(Arrays.asList(
                section("1", "Greeting","Dear {customer_name},"),
                section("2", "Exceeding Threshould", "Your balance exceeds the threshold of LKR {overdue_threshold}"),
                section("3", "Disconnection Notice", "The Pradeshiya Sabha can disconnect the water line if payment is missed."),
                section("4", "Reconnection Fee", "After disconnection, an additional charge of LKR {reconnection_fee} will be applied for reconnection.")
                ));

        MessageTemplateDto email = new MessageTemplateDto();
        email.setIsCustom(false);
        email.setContent("");
        email.setSections(Arrays.asList());

        TemplatesDto templates = new TemplatesDto();
        templates.setSms(sms);
        templates.setEmail(email);
        dto.setTemplates(templates);

        return dto;
    }

    private ScheduledMessageDto buildWaterCutoff() {
        ScheduledMessageDto dto = new ScheduledMessageDto();
        dto.setName("Water supply cut-off");
        dto.setChannels(Arrays.asList("SMS"));
        dto.setIsDefault(true);
        dto.setRecipients("Selected Customers");

        ScheduleDto schedule = new ScheduleDto();
        schedule.setType("One-Time");
        schedule.setDate(LocalDate.parse("2026-01-15"));
        schedule.setTime(LocalTime.of(10, 0));
        dto.setSchedule(schedule);

        MessageTemplateDto sms = new MessageTemplateDto();
        sms.setIsCustom(true);
        sms.setContent("Water supply will be interrupted tomorrow 8am-5pm for maintenance.");
        sms.setSections(Arrays.asList());

        MessageTemplateDto email = new MessageTemplateDto();
        email.setIsCustom(true);
        email.setContent("");
        email.setSections(Arrays.asList());

        TemplatesDto templates = new TemplatesDto();
        templates.setSms(sms);
        templates.setEmail(email);
        dto.setTemplates(templates);

        return dto;
    }

    private ScheduledMessageDto buildCustomMessageA() {
        ScheduledMessageDto dto = new ScheduledMessageDto();
        dto.setName("Custom message A");
        dto.setChannels(Arrays.asList("SMS"));
        dto.setIsDefault(false);
        dto.setRecipients("All Customers");

        ScheduleDto schedule = new ScheduleDto();
        schedule.setType("One-Time");
        schedule.setDate(LocalDate.parse("2026-02-01"));
        schedule.setTime(LocalTime.of(12, 0));
        dto.setSchedule(schedule);

        MessageTemplateDto sms = new MessageTemplateDto();
        sms.setIsCustom(true);
        sms.setContent("Happy Independence Day!");
        sms.setSections(Arrays.asList());

        MessageTemplateDto email = new MessageTemplateDto();
        email.setIsCustom(true);
        email.setContent("");
        email.setSections(Arrays.asList());

        TemplatesDto templates = new TemplatesDto();
        templates.setSms(sms);
        templates.setEmail(email);
        dto.setTemplates(templates);

        return dto;
    }

    private TemplateSectionDto section(String id, String name, String content) {
        TemplateSectionDto s = new TemplateSectionDto();
        s.setId(id);
        s.setName(name);
        s.setContent(content);
        return s;
    }
}
