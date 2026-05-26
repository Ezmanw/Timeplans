## 2024-05-21 - Duplicate Class Quick Action
**Learning:** Users often have the same class on multiple days or times. The ability to duplicate a class from the Timetable action dialog saves time rather than re-entering all details.
**Action:** Implemented a Duplicate action next to Edit in the TimetableScreen action dialog which triggers the LessonAddEditDialog with the selected lesson's fields prefilled but ID set to 0.
## 2024-05-22 - Clear Completed Tasks Bulk Action
**Learning:** Users who frequently complete tasks often clutter their lists. A "Clear Done" bulk action provides a quick way to clean up the interface, saving them from having to delete items one by one.
**Action:** Implemented a bulk delete action for tasks that are marked as completed. The button only appears when completed tasks are present, keeping the UI clean.
## 2024-05-26 - Edit Tasks and Exams Quality-of-Life Feature
**Learning:** For a task/exam tracking application, the absence of edit functionality causes user friction as users are forced to delete and re-enter details for minor mistakes or changes. Reusing existing 'Add' dialogs by passing an optional task-to-edit parameter provides a clean pattern for implementing edit support with minimal UI duplication.
**Action:** Implemented edit capability for Homework, Revision, and Exams by augmenting the existing create dialogs with `taskToEdit` / `examToEdit` state parameters, adding an edit icon button to the list rows, and creating corresponding `update` repository methods.
