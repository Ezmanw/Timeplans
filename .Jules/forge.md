## 2024-05-21 - Duplicate Class Quick Action
**Learning:** Users often have the same class on multiple days or times. The ability to duplicate a class from the Timetable action dialog saves time rather than re-entering all details.
**Action:** Implemented a Duplicate action next to Edit in the TimetableScreen action dialog which triggers the LessonAddEditDialog with the selected lesson's fields prefilled but ID set to 0.
## 2024-05-22 - Clear Completed Tasks Bulk Action
**Learning:** Users who frequently complete tasks often clutter their lists. A "Clear Done" bulk action provides a quick way to clean up the interface, saving them from having to delete items one by one.
**Action:** Implemented a bulk delete action for tasks that are marked as completed. The button only appears when completed tasks are present, keeping the UI clean.
## 2024-05-18 - Replacing string inputs with dialogs for Dates
**Learning:** Hardcoded manual input fields for dates are annoying. Since we are using Jetpack Compose, an Android `DatePickerDialog` combined with a `readOnly` `OutlinedTextField` trailing icon is a good reusable pattern for replacing manual date typings. Ensure the correct fully qualified classes are used for imports.
**Action:** When working on features requiring date entries, avoid manual input strings and leverage standard UI component dialogs (like `DatePickerDialog`) instead.
