## 2024-05-21 - Duplicate Class Quick Action
**Learning:** Users often have the same class on multiple days or times. The ability to duplicate a class from the Timetable action dialog saves time rather than re-entering all details.
**Action:** Implemented a Duplicate action next to Edit in the TimetableScreen action dialog which triggers the LessonAddEditDialog with the selected lesson's fields prefilled but ID set to 0.
## 2024-05-24 - Filter UI Overflow Safety
**Learning:** When adding horizontal elements like FilterChips to an Android UI, standard `Row` containers can easily overflow on smaller screens.
**Action:** Replace `Row` with `LazyRow` when implementing horizontal lists of interactive chips or options to ensure they remain scrollable and accessible.
