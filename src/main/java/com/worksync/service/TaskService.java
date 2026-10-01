package com.worksync.service;

import com.worksync.model.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public List<Task> getTasksByUser(String uid) {
        return tasks.values().stream()
                .filter(task -> uid.equals(task.getAssignedToUid()))
                .collect(Collectors.toList());
    }

    public String createTask(Task task, String assignedByUid) {
        String taskId = UUID.randomUUID().toString();
        task.setId(taskId);
        task.setStatus("PENDING");
        task.setAssignedByUid(assignedByUid);
        tasks.put(taskId, task);
        return taskId;
    }

    public List<Task> getAllTasks() {
        return List.copyOf(tasks.values());
    }

    public Task getTaskById(String id) {
        return tasks.get(id);
    }

    public void updateTask(Task task) {
        if (task == null || task.getId() == null) throw new IllegalArgumentException("Task does not exist");
        tasks.put(task.getId(), task);
    }

    public void deleteTaskById(String id) {
        tasks.remove(id);
    }

    public List<Task> getTasksByUserAndStatus(String uid, String status, String sort) {
        return getTasksByUserFilteredAndSorted(uid, status, sort);
    }

    public List<Task> getTasksByUserFilteredAndSorted(String uid, String status, String sort) {
        Comparator<Task> comparator = Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("status".equals(sort)) {
            comparator = Comparator.comparing(task -> switch (task.getStatus()) {
                case "PENDING" -> 1;
                case "IN_PROGRESS" -> 2;
                case "COMPLETED" -> 3;
                default -> 4;
            });
        }

        return getTasksByUser(uid).stream()
                .filter(task -> !task.isArchived())
                .filter(task -> status == null || status.isBlank() || status.equals(task.getStatus()))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<Task> getUrgentTasks(String uid) {
        return getTasksByUser(uid).stream()
                .filter(Task::isUrgent)
                .filter(task -> !"COMPLETED".equalsIgnoreCase(task.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Task> getDueTasks(String uid) {
        LocalDate today = LocalDate.now();
        return getTasksByUser(uid).stream()
                .filter(task -> !"COMPLETED".equalsIgnoreCase(task.getStatus()))
                .filter(task -> {
                    try {
                        return task.getDeadline() != null && !LocalDate.parse(task.getDeadline()).isAfter(today);
                    } catch (RuntimeException ignored) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    public List<Task> getRecentTasks(String uid) {
        return getTasksByUser(uid).stream()
                .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
    }
}
