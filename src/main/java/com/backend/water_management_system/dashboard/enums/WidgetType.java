package com.backend.water_management_system.dashboard.enums;

/**
 * Controlled widget type taxonomy.
 * The frontend uses this to determine rendering behaviour.
 */
public enum WidgetType {
    /** Displays a single numeric/text statistic. */
    STAT,
    /** Displays a chart (line, bar, area, etc.). */
    CHART,
    /** Displays a tabular data set. */
    TABLE,
    /** Displays an ordered or unordered list of items. */
    LIST,
    /** Displays an alert / warning banner. */
    ALERT,
    /** A call-to-action / navigation shortcut. */
    ACTION,
    /** Displays a progress indicator. */
    PROGRESS,
    /** Displays an announcement or blog excerpt. */
    ANNOUNCEMENT
}
