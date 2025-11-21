package project.fitnessapplication.restday.model;

public enum RestDayReason {
    PLANNED("Planned Rest"),
    SICK("Sick"),
    TRAVEL("Travel"),
    INJURY("Injury"),
    WORK("Work Commitments"),
    FAMILY("Family Time"),
    OTHER("Other");

    private final String displayName;

    RestDayReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
