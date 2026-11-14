package ua.minibank.api;

import java.util.List;

/** Сторінка результатів. total — скільки всього елементів, щоб клієнт міг показати пагінацію. */
public record PageDto<T>(List<T> items, int page, int size, long total) {
}
