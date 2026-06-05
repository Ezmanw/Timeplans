## 2024-05-21 - Duplicate Class Quick Action
**Learning:** Users often have the same class on multiple days or times. The ability to duplicate a class from the Timetable action dialog saves time rather than re-entering all details.
**Action:** Implemented a Duplicate action next to Edit in the TimetableScreen action dialog which triggers the LessonAddEditDialog with the selected lesson's fields prefilled but ID set to 0.
## 2024-05-22 - Clear Completed Tasks Bulk Action
**Learning:** Users who frequently complete tasks often clutter their lists. A "Clear Done" bulk action provides a quick way to clean up the interface, saving them from having to delete items one by one.
**Action:** Implemented a bulk delete action for tasks that are marked as completed. The button only appears when completed tasks are present, keeping the UI clean.

## 2024-05-23 - Share Action for Tasks
**Learning:** Users often need to share homework or revision schedules with classmates. Providing a simple share intent on task items is a high-value quick action that facilitates collaboration and saves manual copy-pasting.
**Action:** Implemented a Share button on Homework and Revision tasks in the TasksScreen that launches an Android ACTION_SEND intent with formatted task details.

## 2026-06-05 - Task List Filtering
**Learning:** When users accumulate a large number of tasks over a school term, the task list becomes cluttered and hard to navigate, even with sorting options. Hiding completed items and filtering by a specific subject allows users to focus on what needs to be done for a particular class without being overwhelmed.
**Action:** Added a horizontally scrollable chip row in TasksScreen that includes a 'Hide Done' toggle and a 'Subject Filter' dropdown. Modified the underlying `filteredSortedTasks` state list to apply these filters before the sorting logic.
