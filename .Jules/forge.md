## 2024-05-21 - Duplicate Class Quick Action
**Learning:** Users often have the same class on multiple days or times. The ability to duplicate a class from the Timetable action dialog saves time rather than re-entering all details.
**Action:** Implemented a Duplicate action next to Edit in the TimetableScreen action dialog which triggers the LessonAddEditDialog with the selected lesson's fields prefilled but ID set to 0.
## 2024-05-22 - Clear Completed Tasks Bulk Action
**Learning:** Users who frequently complete tasks often clutter their lists. A "Clear Done" bulk action provides a quick way to clean up the interface, saving them from having to delete items one by one.
**Action:** Implemented a bulk delete action for tasks that are marked as completed. The button only appears when completed tasks are present, keeping the UI clean.
## 2024-05-23 - Dynamic Default Dates
**Learning:** Hardcoded default dates in dialogs (e.g., '2026-06-15') lead to a frustrating UX since users constantly have to adjust them by a large margin manually or rewrite them entirely.
**Action:** Replaced static date strings with a dynamic calculation () and added a  to make inputting dates less error-prone.
## 2024-05-23 - Dynamic Default Dates
**Learning:** Hardcoded default dates in dialogs (e.g., "2026-06-15") lead to a frustrating UX since users constantly have to adjust them by a large margin manually or rewrite them entirely.
**Action:** Replaced static date strings with a dynamic calculation (`Calendar.getInstance()`) and added a `DatePickerDialog` to make inputting dates less error-prone.
