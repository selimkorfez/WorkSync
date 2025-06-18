package com.worksync.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.worksync.model.Task;
import com.worksync.model.TimeEntry;
import com.worksync.model.User;
import com.worksync.service.TaskService;
import com.worksync.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    private final Firestore firestore = FirestoreClient.getFirestore();



    @GetMapping("/form")
    public String showForm(HttpServletRequest request, Model model) {
        String uid = (String) request.getSession().getAttribute("uid");

        if (uid == null || uid.isBlank()) {
            System.out.println("⚠️ UID is missing when rendering task form.");
            return "redirect:/login"; // or show an error page
        }

        model.addAttribute("task", new Task());
        model.addAttribute("uid", uid); // available to Thymeleaf
        return "create-task";
    }


    @PostMapping("/submit")
    public String submitForm(@ModelAttribute Task task,
                             @RequestParam("uid") String uid,
                             RedirectAttributes redirectAttributes) {
        try {
            // Enforce today's deadline if blank or invalid
            if (task.getDeadline() == null || task.getDeadline().isBlank()) {
                task.setDeadline(LocalDate.now().toString());
            } else {
                LocalDate parsed = LocalDate.parse(task.getDeadline());
                if (parsed.isBefore(LocalDate.now())) {
                    task.setDeadline(LocalDate.now().toString());
                }
            }

            // Set creator manually (don’t trust form binding)
            task.setAssignedByUid(uid);

            // Logging (diagnostic)
            System.out.println("🔥 [TaskController] Creating task:");
            System.out.println("   - Title: " + task.getTitle());
            System.out.println("   - Description: " + task.getDescription());
            System.out.println("   - Assigned To UID: " + task.getAssignedToUid());
            System.out.println("   - Assigned By UID: " + task.getAssignedByUid());
            System.out.println("   - Deadline: " + task.getDeadline());

            // Create the task
            taskService.createTask(task, uid);

            return "redirect:/tasks/form?success=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/tasks/form?error=true";
        }
    }

    @GetMapping("/")
    public String showDashboard(@RequestParam("uid") String uid, Model model)
            throws ExecutionException, InterruptedException {
        List<Task> urgentTasks = taskService.getUrgentTasks(uid);
        List<Task> dueSoon = taskService.getDueTasks(uid);
        List<Task> recent = taskService.getRecentTasks(uid);

        model.addAttribute("urgentTasks", urgentTasks);
        model.addAttribute("dueTasks", dueSoon);
        model.addAttribute("recentTasks", recent);

        return "homepage";
    }


    @GetMapping("/list")
    public String listTasks(@RequestParam(value = "uid", required = false) String uid,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "sort", required = false) String sort,
                            Model model) {
        try {
            List<Task> tasks = new ArrayList<>();



            if (uid != null && !uid.isEmpty()) {
                tasks = taskService.getTasksByUserFilteredAndSorted(uid, status, sort);
            }

            // UID to FullName map for display
            Map<String, String> uidToName = new HashMap<>();
            for (Task task : tasks) {
                String assigneeUid = task.getAssignedToUid();
                if (assigneeUid != null && !assigneeUid.isBlank() && !uidToName.containsKey(assigneeUid)) {
                    User user = userService.getByUid(assigneeUid);
                    uidToName.put(assigneeUid, user != null ? user.getFullName() : "Unnamed User");
                }

                String creatorUid = task.getAssignedByUid();
                if (creatorUid != null && !creatorUid.isBlank() && !uidToName.containsKey(creatorUid)) {
                    try {
                        User user = userService.getByUid(creatorUid);
                        uidToName.put(creatorUid, user != null ? user.getFullName() : "Unnamed User");
                    } catch (Exception e) {
                        System.out.println("⚠️ Invalid creator UID: '" + creatorUid + "' for task " + task.getTitle());
                        uidToName.put(creatorUid, "Unknown");
                    }
                }


            }


            model.addAttribute("tasks", tasks);
            model.addAttribute("uidToName", uidToName); //  for Thymeleaf
            model.addAttribute("uid", uid == null ? "" : uid);
            model.addAttribute("status", status);
            model.addAttribute("sort", sort);

            return "task-list";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("tasks", new ArrayList<>());
            System.out.println("⚠️ Error rendering task list page.");
            return "task-list";
        }
    }

    @GetMapping("/edit/{id}")
    public String editTask(@PathVariable String id,
                           @RequestParam("uid") String uid,
                           Model model) {
        try {
            Task task = taskService.getTaskById(id);
            String role = userService.getRoleByUid(uid);

            model.addAttribute("task", task);
            model.addAttribute("uid", uid);
            model.addAttribute("userRole", role);

            return "edit-task";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/tasks/list?uid=" + uid + "&refresh=" + System.currentTimeMillis();
        }
    }


    @PostMapping("/update")
    public String updateTask(@ModelAttribute Task updatedTask,
                             @RequestParam("uid") String uid,
                             HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            String role = userService.getRoleByUid(uid);
            Task existing = taskService.getTaskById(updatedTask.getId());

            if ("ADMIN".equalsIgnoreCase(role)) {
                existing.setTitle(updatedTask.getTitle());
                existing.setDescription(updatedTask.getDescription());
                existing.setDeadline(updatedTask.getDeadline());
            }

            String newStatus = updatedTask.getStatus();
            String oldStatus = existing.getStatus();
            existing.setStatus(newStatus);

//  If just switched to IN_PROGRESS → record start time
            if ("IN_PROGRESS".equalsIgnoreCase(newStatus) && !"IN_PROGRESS".equalsIgnoreCase(oldStatus)) {
                existing.setInProgressStart(LocalDateTime.now().toString());
            }

//  If just switched to COMPLETED → record completion time
            if ("COMPLETED".equalsIgnoreCase(newStatus) && !"COMPLETED".equalsIgnoreCase(oldStatus)) {
                existing.setCompletionTime(LocalDateTime.now().toString());
            }


            //  EMPLOYEE: Add time entry if form is filled
            if ("EMPLOYEE".equalsIgnoreCase(role)) {
                String logDate = request.getParameter("logDate");
                String logHours = request.getParameter("logHours");
                String logNotes = request.getParameter("logNotes");

                if (logDate != null && logHours != null && !logDate.isBlank() && !logHours.isBlank()) {
                    try {
                        double hours = Double.parseDouble(logHours);
                        TimeEntry entry = new TimeEntry(uid, logDate, hours, logNotes);
                        List<TimeEntry> entries = existing.getTimeEntries();
                        entries.add(entry);
                        existing.setTimeEntries(entries);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Save the task
            taskService.updateTask(existing);
            redirectAttributes.addFlashAttribute("success", "Task updated successfully.");
            return "redirect:/tasks/list?uid=" + uid;

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("success", "Task deleted successfully.");
            return "redirect:/tasks/list?uid=" + uid + "&refresh=" + System.currentTimeMillis();
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteTask(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            taskService.deleteTaskById(id);
            redirectAttributes.addFlashAttribute("success", "Task deleted successfully.");
            return "redirect:/tasks/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("success", "Task deleted successfully.");
            return "redirect:/tasks/list?error=true";
        }
    }
    @GetMapping("/dashboard")
    public String userDashboard(@RequestParam("uid") String uid, Model model)
            throws ExecutionException, InterruptedException {

        List<Task> allTasks = taskService.getTasksByUser(uid);

        // Filter tasks
        List<Task> completed = allTasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .collect(Collectors.toList());
        DateTimeFormatter input = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        DateTimeFormatter output = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Task task : completed) {
            try {
                if (task.getCompletionTime() != null && !task.getCompletionTime().isBlank()) {
                    LocalDateTime parsed = LocalDateTime.parse(task.getCompletionTime(), input);
                    task.setCompletionTimeFormatted(output.format(parsed));
                }
            } catch (Exception e) {
                System.out.println("⚠️ Failed to format completionTime for: " + task.getTitle());
            }
        }


        List<Task> late = allTasks.stream()
                .filter(t -> t.isLate() && !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .collect(Collectors.toList());

        // Calculate average duration
        double avgDuration = completed.stream()
                .filter(t -> t.getInProgressStart() != null && t.getCompletionTime() != null)
                .mapToLong(t -> {
                    try {
                        LocalDateTime start = LocalDateTime.parse(t.getInProgressStart());
                        LocalDateTime end = LocalDateTime.parse(t.getCompletionTime());
                        return java.time.Duration.between(start, end).toMinutes();
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .filter(d -> d > 0)
                .average()
                .orElse(0);

        model.addAttribute("completedCount", completed.size());
        model.addAttribute("lateCount", late.size());
        // Convert avgDuration (in minutes) to a readable format like "1 day 2 hrs 15 mins"
        long totalMins = (long) avgDuration;
        long days = totalMins / (60 * 24);
        long hours = (totalMins % (60 * 24)) / 60;
        long mins = totalMins % 60;

        StringBuilder formatted = new StringBuilder();
        if (days > 0) formatted.append(days).append(" day").append(days > 1 ? "s " : " ");
        if (hours > 0) formatted.append(hours).append(" hr").append(hours > 1 ? "s " : " ");
        if (mins > 0 || formatted.length() == 0) formatted.append(mins).append(" min").append(mins > 1 ? "s" : "");

        model.addAttribute("avgDurationFormatted", formatted.toString().trim());
        String healthStatus = "Good";
        String healthColor = "success"; // Bootstrap green

        if (avgDuration > 2880) {
            healthStatus = "Poor";
            healthColor = "danger"; // Bootstrap red
        } else if (avgDuration > 1440) {
            healthStatus = "Okay";
            healthColor = "warning"; // Bootstrap yellow
        }

        model.addAttribute("healthStatus", healthStatus);
        model.addAttribute("healthColor", healthColor);
        model.addAttribute("completedTasks", completed);
        model.addAttribute("lateTasks", late);
        model.addAttribute("uid", uid);

        return "dashboard";
    }
    @GetMapping("/admin/dashboard")
    public String adminDashboard(@RequestParam("uid") String uid, Model model) throws ExecutionException, InterruptedException {
        String role = userService.getRoleByUid(uid);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return "redirect:/?unauthorized=true"; // 🚫 prevent access
        }

        List<Task> allTasks = taskService.getAllTasks();

        // Count completed and pending
        long completed = allTasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        long pending = allTasks.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus())).count();
        long inProgress = allTasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long late = allTasks.stream().filter(t -> t.isLate()).count();

        // Group tasks by user UID
        Map<String, Long> taskCountByUser = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getAssignedToUid, Collectors.counting()));

        // Map UID to full name
        Map<String, String> uidToName = new HashMap<>();
        for (String userId : taskCountByUser.keySet()) {
            User user = userService.getByUid(userId);
            if (user != null) {
                uidToName.put(userId, user.getFullName());
            }
        }

        model.addAttribute("uid", uid);
        model.addAttribute("taskCountByUser", taskCountByUser);
        model.addAttribute("uidToName", uidToName);
        model.addAttribute("completedCount", completed);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("lateCount", late);

        return "admin-dashboard";
    }
    @GetMapping("/notifications")
    @ResponseBody
    public Map<String, List<Task>> getStyledNotifications(@RequestParam String uid) throws ExecutionException, InterruptedException {
        List<Task> tasks = taskService.getAllTasks();
        LocalDate today = LocalDate.now();

        Map<String, List<Task>> grouped = new HashMap<>();
        grouped.put("urgent", new ArrayList<>());
        grouped.put("dueToday", new ArrayList<>());
        grouped.put("overdue", new ArrayList<>());

        Set<String> addedTaskIds = new HashSet<>();

        for (Task task : tasks) {
            if (!uid.equals(task.getAssignedToUid()) || !"PENDING".equalsIgnoreCase(task.getStatus()) || task.getId() == null)
                continue;

            try {
                // 🔥 Priority 1: Urgent
                if (task.isUrgent() && !addedTaskIds.contains(task.getId())) {
                    grouped.get("urgent").add(task);
                    addedTaskIds.add(task.getId());
                    continue;
                }

                // ⏰ Priority 2: Due Today
                if (task.getDeadline() != null && !task.getDeadline().isEmpty()) {
                    LocalDate deadline = LocalDate.parse(task.getDeadline());
                    if (deadline.isEqual(today) && !addedTaskIds.contains(task.getId())) {
                        grouped.get("dueToday").add(task);
                        addedTaskIds.add(task.getId());
                        continue;
                    }

                    // ⚠️ Priority 3: Overdue
                    if (deadline.isBefore(today) && !addedTaskIds.contains(task.getId())) {
                        grouped.get("overdue").add(task);
                        addedTaskIds.add(task.getId());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return grouped;
    }
        @GetMapping("/api/tasks/preview")
    @ResponseBody
    public Map<String, List<Task>> getPreviewTasks(@RequestParam String uid) throws ExecutionException, InterruptedException {
        Map<String, List<Task>> preview = new HashMap<>();
        preview.put("urgent", taskService.getUrgentTasks(uid));
        preview.put("upcoming", taskService.getDueTasks(uid));
        return preview;
    }

    @GetMapping("/assigned")
    public String viewAssignedTasks(HttpServletRequest request, Model model) throws ExecutionException, InterruptedException {
        String uid = (String) request.getSession().getAttribute("uid");

        if (uid == null || uid.isBlank()) {
            return "redirect:/login"; // or error page
        }

        String role = userService.getRoleByUid(uid);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return "redirect:/?unauthorized=true";
        }

        List<Task> tasks = taskService.getAllTasks().stream()
                .filter(t -> uid.equals(t.getAssignedByUid()))
                .collect(Collectors.toList());

        Map<String, String> uidToName = new HashMap<>();
        for (Task task : tasks) {
            String assigneeUid = task.getAssignedToUid();
            if (assigneeUid != null && !uidToName.containsKey(assigneeUid)) {
                User user = userService.getByUid(assigneeUid);
                uidToName.put(assigneeUid, user != null ? user.getFullName() : "Unknown User");
            }
        }

        model.addAttribute("uid", uid);
        model.addAttribute("assignedTasks", tasks);
        model.addAttribute("uidToName", uidToName);
        model.addAttribute("userRole", role);

        return "assigned-tasks";
    }
    @PostMapping("/archive/{id}")
    public String archiveTask(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        DocumentSnapshot snapshot = firestore.collection("tasks").document(id).get().get();
        if (snapshot.exists()) {
            Task task = snapshot.toObject(Task.class);
            if (task != null && "COMPLETED".equals(task.getStatus())) {
                task.setArchived(true);
                firestore.collection("tasks").document(id).set(task);
            }
        }
        redirectAttributes.addFlashAttribute("success", "Task archived successfully.");
        return "redirect:/tasks/list?uid=" + uid;
    }

    @GetMapping("/archived")
    public String viewArchivedTasks(Model model, HttpSession session) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        List<Task> archivedTasks = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = firestore.collection("tasks").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            Task task = doc.toObject(Task.class);
            if (task.isArchived() && uid.equals(task.getAssignedToUid())) {
                archivedTasks.add(task);
            }
        }


        // Resolve UID to Full Name
        Map<String, String> uidToName = new HashMap<>();
        for (Task task : archivedTasks) {
            String assigneeUid = task.getAssignedToUid();
            if (assigneeUid != null && !uidToName.containsKey(assigneeUid)) {
                User user = userService.getByUid(assigneeUid);
                uidToName.put(assigneeUid, user != null ? user.getFullName() : "Unnamed User");
            }

            String creatorUid = task.getAssignedByUid();
            if (creatorUid != null && !uidToName.containsKey(creatorUid)) {
                User user = userService.getByUid(creatorUid);
                uidToName.put(creatorUid, user != null ? user.getFullName() : "Unknown");
            }
        }

        model.addAttribute("archivedTasks", archivedTasks);
        model.addAttribute("uidToName", uidToName);

        return "archived-task-list";
    }
    @PostMapping("/unarchive/{id}")
    public String unarchiveTask(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        DocumentSnapshot snapshot = firestore.collection("tasks").document(id).get().get();
        if (snapshot.exists()) {
            Task task = snapshot.toObject(Task.class);
            if (task != null && task.isArchived()) {
                task.setArchived(false);
                firestore.collection("tasks").document(id).set(task);
                redirectAttributes.addFlashAttribute("success", "Task unarchived successfully.");
            }
        }
        redirectAttributes.addFlashAttribute("success", "Task unarchived successfully.");
        return "redirect:/tasks/archived";
    }

}
