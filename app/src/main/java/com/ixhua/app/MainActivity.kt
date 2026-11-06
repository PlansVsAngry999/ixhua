package com.ixhua.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ixhua.app.data.ListItem
import com.ixhua.app.data.Task
import com.ixhua.app.data.TaskStorage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// COLORES IXHUA

private val Black = Color(0xFF09080D)
private val Card = Color(0xFF17141F)
private val CardLight = Color(0xFF241C32)
private val Purple = Color(0xFFB65CFF)
private val PurpleDark = Color(0xFF7135B5)
private val White = Color(0xFFF7F2FC)
private val Gray = Color(0xFF9991A5)
private val Green = Color(0xFF65D6A0)
private val Red = Color(0xFFFF7373)
private val Orange = Color(0xFFFFB65C)

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IxhuaApp()
        }
    }
}

@Composable
fun IxhuaApp() {
    val context = LocalContext.current

    // SOLICITAR PERMISO DE NOTIFICACIONES

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    LaunchedEffect(Unit) {
        ReminderScheduler.createNotificationChannel(context)

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    var totalXp by remember {
        mutableIntStateOf(TaskStorage.loadTotalXp(context))
    }

    LaunchedEffect(totalXp) {
        TaskStorage.saveTotalXp(context, totalXp)
    }

    val tasks = remember {
        mutableStateListOf<Task>().apply {
            addAll(TaskStorage.loadTasks(context))
        }
    }

    val lists = remember {
        mutableStateListOf<ListItem>().apply {
            addAll(TaskStorage.loadLists(context))

            if (none { it.id == "default" }) {
                add(
                    0,
                    ListItem(
                        id = "default",
                        name = "Mis tareas"
                    )
                )
            }
        }
    }

    // COMPLETAR TAREA Y OTORGAR XP

    fun handleTaskCompletion(task: Task, checked: Boolean) {
        val index = tasks.indexOfFirst {
            it.id == task.id
        }

        if (index == -1) return

        val currentTask = tasks[index]

        val earnsXp = checked &&
                !currentTask.completed &&
                !currentTask.xpAwarded

        tasks[index] = currentTask.copy(
            completed = checked,
            xpAwarded = currentTask.xpAwarded || earnsXp
        )

        if (earnsXp) {
            totalXp += 10
        }
    }

    // ELIMINAR TAREA Y DESCONTAR XP SI CORRESPONDE

    fun handleTaskDeletion(task: Task) {
        val index = tasks.indexOfFirst {
            it.id == task.id
        }

        if (index == -1) return

        val taskToDelete = tasks[index]

        ReminderScheduler.cancel(context, taskToDelete.id)

        if (taskToDelete.xpAwarded) {
            totalXp = (totalXp - 10).coerceAtLeast(0)
        }

        tasks.removeAt(index)
    }

    // GUARDAR TAREAS Y SINCRONIZAR RECORDATORIOS

    LaunchedEffect(tasks.toList()) {
        val currentTasks = tasks.toList()

        TaskStorage.saveTasks(context, currentTasks)

        currentTasks.forEach { task ->
            if (
                task.completed ||
                task.reminderAt == null
            ) {
                ReminderScheduler.cancel(context, task.id)
            } else {
                ReminderScheduler.schedule(context, task)
            }
        }
    }

    LaunchedEffect(lists.toList()) {
        TaskStorage.saveLists(context, lists.toList())
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var showListsDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Black,
            surface = Card,
            primary = Purple
        )
    ) {
        Scaffold(
            containerColor = Black,

            bottomBar = {
                NavigationBar(
                    containerColor = Card,
                    contentColor = White
                ) {
                    val tabs = listOf(
                        "Inicio",
                        "Tareas",
                        "Estadísticas",
                        "Perfil"
                    )

                    val icons = listOf(
                        Icons.Default.Home,
                        Icons.Default.CheckCircle,
                        Icons.Default.BarChart,
                        Icons.Default.Person
                    )

                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    icons[index],
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(label, fontSize = 10.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Purple,
                                selectedTextColor = Purple,
                                unselectedIconColor = Gray,
                                unselectedTextColor = Gray,
                                indicatorColor = CardLight
                            )
                        )
                    }
                }
            },

            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    FloatingActionButton(
                        onClick = {
                            editingTask = null
                            showDialog = true
                        },
                        containerColor = Purple,
                        contentColor = Black,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Nueva tarea"
                        )
                    }
                }
            }
        ) { padding ->

            when (selectedTab) {
                0 -> HomeScreen(
                    tasks = tasks,
                    totalXp = totalXp,
                    padding = padding,
                    onViewAll = { selectedTab = 1 },
                    onEdit = {
                        editingTask = it
                        showDialog = true
                    },
                    onDelete = { task ->
                        handleTaskDeletion(task)
                    },
                    onComplete = { task, checked ->
                        handleTaskCompletion(task, checked)
                    }
                )

                1 -> TasksScreen(
                    tasks = tasks,
                    lists = lists,
                    padding = padding,
                    onEdit = {
                        editingTask = it
                        showDialog = true
                    },
                    onDelete = { task ->
                        handleTaskDeletion(task)
                    },
                    onComplete = { task, checked ->
                        handleTaskCompletion(task, checked)
                    },
                    onManageLists = {
                        showListsDialog = true
                    }
                )

                2 -> StatisticsScreen(
                    tasks = tasks,
                    totalXp = totalXp,
                    padding = padding
                )

                3 -> ProfileScreen(
                    totalXp = totalXp,
                    tasks = tasks,
                    padding = padding
                )
            }
        }

        if (showDialog) {
            TaskDialog(
                initialTask = editingTask,
                lists = lists,
                onDismiss = {
                    showDialog = false
                    editingTask = null
                },
                onSave = { task ->
                    val index = tasks.indexOfFirst {
                        it.id == task.id
                    }

                    if (index >= 0) {
                        tasks[index] = task
                    } else {
                        tasks.add(task)
                    }

                    showDialog = false
                    editingTask = null
                }
            )
        }

        if (showListsDialog) {
            ListManagerDialog(
                lists = lists,
                onDismiss = {
                    showListsDialog = false
                },
                onAdd = { name ->
                    lists.add(ListItem(name = name))
                },
                onRename = { list, newName ->
                    val index = lists.indexOfFirst {
                        it.id == list.id
                    }

                    if (index >= 0) {
                        lists[index] = list.copy(name = newName)
                    }
                },
                onDelete = { list ->
                    tasks.indices.forEach { index ->
                        if (tasks[index].listId == list.id) {
                            tasks[index] = tasks[index].copy(
                                listId = "default"
                            )
                        }
                    }

                    lists.removeAll {
                        it.id == list.id
                    }
                }
            )
        }
    }
}

// ENCABEZADO

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Purple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Black
                )
            }

            Text(
                "IXHUA",
                color = White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
        }

        Text(
            "ORGANIZA TU TIEMPO",
            color = Gray,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

// INICIO

@Composable
fun HomeScreen(
    tasks: List<Task>,
    totalXp: Int,
    padding: PaddingValues,
    onViewAll: () -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onComplete: (Task, Boolean) -> Unit
) {
    val completed = tasks.count { it.completed }

    val progress = if (tasks.isEmpty()) 0f
    else completed.toFloat() / tasks.size

    val level = (totalXp / 100) + 1
    val xpInLevel = totalXp % 100

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 22.dp),
        contentPadding = PaddingValues(
            top = 25.dp,
            bottom = 35.dp
        ),
        verticalArrangement = Arrangement.spacedBy(23.dp)
    ) {
        item {
            Header()
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    "TU ESPACIO PERSONAL",
                    color = Purple,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )

                Text(
                    "Organiza tu día.",
                    color = White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Un paso a la vez. Tú tienes el control.",
                    color = Gray,
                    fontSize = 12.sp
                )
            }
        }

        item {
            Surface(
                color = CardLight,
                shape = RoundedCornerShape(17.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Purple
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "NIVEL $level",
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Text(
                                "Tu aventura continúa",
                                color = Gray,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            "$totalXp XP",
                            color = Purple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    LinearProgressIndicator(
                        progress = { xpInLevel / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                        color = Purple,
                        trackColor = Black
                    )

                    Text(
                        "$xpInLevel / 100 XP para el siguiente nivel",
                        color = Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }

        item {
            Surface(
                color = Card,
                shape = RoundedCornerShape(17.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tu progreso",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Text(
                            "${(progress * 100).toInt()}%",
                            color = Purple,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Green,
                        trackColor = CardLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$completed completadas",
                            color = Gray,
                            fontSize = 11.sp
                        )

                        Text(
                            "${tasks.size - completed} pendientes",
                            color = Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tareas recientes",
                    color = White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onViewAll) {
                    Text(
                        "Ver todas",
                        color = Purple,
                        fontSize = 11.sp
                    )
                }
            }
        }

        val recentTasks = tasks
            .filter { !it.completed }
            .take(4)

        if (recentTasks.isEmpty()) {
            item {
                EmptyState(
                    title = "Todo al día",
                    subtitle = "No tienes tareas pendientes."
                )
            }
        } else {
            items(recentTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onCheckedChange = {
                        onComplete(task, it)
                    },
                    onEdit = {
                        onEdit(task)
                    },
                    onDelete = {
                        onDelete(task)
                    }
                )
            }
        }
    }
}

// PANTALLA DE TAREAS

@Composable
fun TasksScreen(
    tasks: List<Task>,
    lists: List<ListItem>,
    padding: PaddingValues,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onComplete: (Task, Boolean) -> Unit,
    onManageLists: () -> Unit
) {
    var selectedFilter by remember {
        mutableStateOf("TODAS")
    }

    val filters = listOf(
        "TODAS",
        "PENDIENTES",
        "COMPLETADAS"
    )

    val filteredTasks = when (selectedFilter) {
        "PENDIENTES" -> tasks.filter { !it.completed }
        "COMPLETADAS" -> tasks.filter { it.completed }
        else -> tasks
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 25.dp,
            bottom = 35.dp
        ),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Header()
        }

        item {
            Text(
                "Mis tareas",
                color = White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            OutlinedButton(
                onClick = onManageLists,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Purple
                )
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Administrar listas")
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            selectedFilter = filter
                        },
                        label = {
                            Text(filter)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleDark,
                            selectedLabelColor = White,
                            labelColor = Gray
                        )
                    )
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                EmptyState(
                    title = "No hay tareas",
                    subtitle = "Agrega una tarea para comenzar."
                )
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onCheckedChange = {
                        onComplete(task, it)
                    },
                    onEdit = {
                        onEdit(task)
                    },
                    onDelete = {
                        onDelete(task)
                    }
                )
            }
        }
    }
}

// TARJETA DE TAREA

@Composable
fun TaskCard(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Purple,
                        uncheckedColor = Gray,
                        checkmarkColor = Black
                    )
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        task.title,
                        color = if (task.completed) Gray else White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        task.category,
                        color = Purple,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar tarea",
                        tint = Purple
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar tarea",
                        tint = Red
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val priorityColor = when (task.priority) {
                    "ALTA" -> Red
                    "MEDIA" -> Orange
                    else -> Green
                }

                Text(
                    "Prioridad: ${task.priority}",
                    color = priorityColor,
                    fontSize = 10.sp
                )

                if (!task.dueDate.isNullOrBlank()) {
                    Text(
                        "Fecha: ${task.dueDate}",
                        color = Gray,
                        fontSize = 10.sp
                    )
                }
            }

            if (task.reminderAt != null) {
                val reminderDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(task.reminderAt),
                    ZoneId.systemDefault()
                )

                Row(
                    modifier = Modifier.padding(start = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(13.dp)
                    )

                    Text(
                        "Aviso: ${reminderDateTime.format(dateFormatter)} · ${reminderDateTime.format(timeFormatter)}",
                        color = Purple,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ESTADO VACÍO

@Composable
fun EmptyState(
    title: String,
    subtitle: String
) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(38.dp)
            )

            Text(
                title,
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                subtitle,
                color = Gray,
                fontSize = 12.sp
            )
        }
    }
}

// OPCIONES DE PERFIL

@Composable
fun ProfileScreen(
    totalXp: Int,
    tasks: List<Task>,
    padding: PaddingValues
) {
    val completed = tasks.count { it.completed }
    val level = (totalXp / 100) + 1

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 22.dp),
        contentPadding = PaddingValues(
            top = 25.dp,
            bottom = 35.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Header()
        }

        item {
            Text(
                "Mi perfil",
                color = White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Surface(
                color = Card,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(55.dp)
                    )

                    Text(
                        "Usuario Ixhua",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Nivel $level",
                        color = Purple,
                        fontSize = 13.sp
                    )

                    Text(
                        "$totalXp XP acumulados",
                        color = Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            ProfileOption(
                Icons.Default.EmojiEvents,
                "Progreso",
                "$completed tareas completadas"
            )
        }

        item {
            ProfileOption(
                Icons.Default.Settings,
                "Configuración",
                "Personaliza tu experiencia"
            )
        }

        item {
            ProfileOption(
                Icons.Default.Info,
                "Acerca de Ixhua",
                "Organiza tu tiempo"
            )
        }
    }
}

@Composable
fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(25.dp)
            )

            Spacer(Modifier.width(13.dp))

            Column {
                Text(
                    title,
                    color = White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Text(
                    subtitle,
                    color = Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ESTADÍSTICAS

@Composable
fun StatisticsScreen(
    tasks: List<Task>,
    totalXp: Int,
    padding: PaddingValues
) {
    val completed = tasks.count { it.completed }
    val pending = tasks.count { !it.completed }
    val highPriority = tasks.count {
        it.priority == "ALTA" && !it.completed
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 22.dp),
        contentPadding = PaddingValues(
            top = 25.dp,
            bottom = 35.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Header()
        }

        item {
            Text(
                "Estadísticas",
                color = White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            StatCard(
                title = "Tareas completadas",
                value = completed.toString(),
                icon = Icons.Default.CheckCircle,
                color = Green
            )
        }

        item {
            StatCard(
                title = "Tareas pendientes",
                value = pending.toString(),
                icon = Icons.Default.PendingActions,
                color = Orange
            )
        }

        item {
            StatCard(
                title = "Prioridad alta",
                value = highPriority.toString(),
                icon = Icons.Default.PriorityHigh,
                color = Red
            )
        }

        item {
            StatCard(
                title = "Experiencia acumulada",
                value = "$totalXp XP",
                icon = Icons.Default.Bolt,
                color = Purple
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp)
            )

            Spacer(Modifier.width(15.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    color = Gray,
                    fontSize = 11.sp
                )

                Text(
                    value,
                    color = White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ADMINISTRADOR DE LISTAS

@Composable
fun ListManagerDialog(
    lists: List<ListItem>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (ListItem, String) -> Unit,
    onDelete: (ListItem) -> Unit
) {
    var newListName by remember {
        mutableStateOf("")
    }

    var editingList by remember {
        mutableStateOf<ListItem?>(null)
    }

    var editingName by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = {
            Text(
                "Administrar listas",
                color = White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = {
                        newListName = it
                    },
                    label = {
                        Text("Nombre de la nueva lista")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                Button(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            onAdd(newListName.trim())
                            newListName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple
                    )
                ) {
                    Text(
                        "Crear lista",
                        color = Black
                    )
                }

                Divider(color = CardLight)

                lists.forEach { list ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (editingList?.id == list.id) {
                            OutlinedTextField(
                                value = editingName,
                                onValueChange = {
                                    editingName = it
                                },
                                label = {
                                    Text("Nuevo nombre")
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors()
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        if (editingName.isNotBlank()) {
                                            onRename(
                                                list,
                                                editingName.trim()
                                            )
                                        }

                                        editingList = null
                                    }
                                ) {
                                    Text("Guardar", color = Green)
                                }

                                TextButton(
                                    onClick = {
                                        editingList = null
                                    }
                                ) {
                                    Text("Cancelar", color = Gray)
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    list.name,
                                    color = White,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        editingList = list
                                        editingName = list.name
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Renombrar",
                                        tint = Purple
                                    )
                                }

                                if (list.id != "default") {
                                    IconButton(
                                        onClick = {
                                            onDelete(list)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cerrar",
                    color = Purple
                )
            }
        }
    )
}

// FORMULARIO DE TAREA

@Composable
fun TaskDialog(
    initialTask: Task?,
    lists: List<ListItem>,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    val context = LocalContext.current

    var title by remember {
        mutableStateOf(initialTask?.title ?: "")
    }

    var category by remember {
        mutableStateOf(initialTask?.category ?: "PERSONAL")
    }

    var priority by remember {
        mutableStateOf(initialTask?.priority ?: "MEDIA")
    }

    var dueDate by remember {
        mutableStateOf(initialTask?.dueDate ?: "")
    }

    var selectedListId by remember {
        mutableStateOf(initialTask?.listId ?: "default")
    }

    var reminderDate by remember {
        mutableStateOf(
            initialTask?.reminderAt?.let {
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(it),
                    ZoneId.systemDefault()
                ).toLocalDate().toString()
            } ?: ""
        )
    }

    var reminderTime by remember {
        mutableStateOf(
            initialTask?.reminderAt?.let {
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(it),
                    ZoneId.systemDefault()
                ).toLocalTime().format(timeFormatter)
            } ?: ""
        )
    }

    val validDate = dueDate.isBlank() || try {
        LocalDate.parse(dueDate)
        true
    } catch (_: Exception) {
        false
    }

    val reminderIsComplete =
        reminderDate.isBlank() && reminderTime.isBlank() ||
                reminderDate.isNotBlank() && reminderTime.isNotBlank()

    val reminderIsFuture = try {
        if (reminderDate.isBlank() || reminderTime.isBlank()) {
            true
        } else {
            LocalDateTime.of(
                LocalDate.parse(reminderDate),
                LocalTime.parse(reminderTime)
            ).isAfter(LocalDateTime.now())
        }
    } catch (_: Exception) {
        false
    }

    val validReminder =
        reminderIsComplete && reminderIsFuture

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = {
            Text(
                if (initialTask == null) "Nueva tarea"
                else "Editar tarea",
                color = White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Nombre de la tarea")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                Text(
                    "Categoría",
                    color = White,
                    fontSize = 12.sp
                )

                val categories = listOf(
                    "PERSONAL",
                    "ESCUELA",
                    "TRABAJO",
                    "OTROS"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(categories) { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = {
                                category = item
                            },
                            label = {
                                Text(item)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleDark,
                                selectedLabelColor = White,
                                labelColor = Gray
                            )
                        )
                    }
                }

                Text(
                    "Prioridad",
                    color = White,
                    fontSize = 12.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf("BAJA", "MEDIA", "ALTA").forEach { item ->
                        FilterChip(
                            selected = priority == item,
                            onClick = {
                                priority = item
                            },
                            label = {
                                Text(item)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (item) {
                                    "ALTA" -> Red
                                    "MEDIA" -> Orange
                                    else -> Green
                                },
                                selectedLabelColor = Black,
                                labelColor = Gray
                            )
                        )
                    }
                }

                Text(
                    "Lista",
                    color = White,
                    fontSize = 12.sp
                )

                lists.forEach { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedListId == list.id) CardLight
                                else Card
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedListId == list.id,
                            onClick = {
                                selectedListId = list.id
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Purple,
                                unselectedColor = Gray
                            )
                        )

                        Text(
                            list.name,
                            color = White,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {
                        dueDate = it
                    },
                    label = {
                        Text("Fecha límite (AAAA-MM-DD)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                Text(
                    "Recordatorio",
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Elige cuándo quieres que Ixhua te avise. Puede ser antes de la fecha límite.",
                    color = Gray,
                    fontSize = 11.sp
                )

                OutlinedButton(
                    onClick = {
                        val currentDate = try {
                            if (reminderDate.isNotBlank()) {
                                LocalDate.parse(reminderDate)
                            } else {
                                LocalDate.now()
                            }
                        } catch (_: Exception) {
                            LocalDate.now()
                        }

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                reminderDate = LocalDate.of(
                                    year,
                                    month + 1,
                                    day
                                ).toString()
                            },
                            currentDate.year,
                            currentDate.monthValue - 1,
                            currentDate.dayOfMonth
                        ).apply {
                            datePicker.minDate = System.currentTimeMillis()
                        }.show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Purple
                    )
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if (reminderDate.isBlank()) {
                            "Elegir fecha del aviso"
                        } else {
                            "Fecha del aviso: ${
                                try {
                                    LocalDate.parse(reminderDate)
                                        .format(dateFormatter)
                                } catch (_: Exception) {
                                    reminderDate
                                }
                            }"
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        val currentTime = try {
                            if (reminderTime.isNotBlank()) {
                                LocalTime.parse(reminderTime)
                            } else {
                                LocalTime.now()
                            }
                        } catch (_: Exception) {
                            LocalTime.now()
                        }

                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                reminderTime = String.format(
                                    "%02d:%02d",
                                    hour,
                                    minute
                                )
                            },
                            currentTime.hour,
                            currentTime.minute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Purple
                    )
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if (reminderTime.isBlank()) {
                            "Elegir hora del aviso"
                        } else {
                            "Hora del aviso: $reminderTime"
                        }
                    )
                }

                if (
                    reminderDate.isNotBlank() ||
                    reminderTime.isNotBlank()
                ) {
                    TextButton(
                        onClick = {
                            reminderDate = ""
                            reminderTime = ""
                        }
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Red
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            "Quitar recordatorio",
                            color = Red
                        )
                    }
                }

                if (!reminderIsComplete) {
                    Text(
                        "Selecciona tanto la fecha como la hora, o quita el recordatorio.",
                        color = Red,
                        fontSize = 10.sp
                    )
                } else if (!reminderIsFuture) {
                    Text(
                        "El recordatorio debe programarse para una fecha y hora futuras.",
                        color = Red,
                        fontSize = 10.sp
                    )
                }

                if (!validDate) {
                    Text(
                        "Utiliza el formato AAAA-MM-DD para la fecha límite.",
                        color = Red,
                        fontSize = 10.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reminderAt = if (
                        reminderDate.isNotBlank() &&
                        reminderTime.isNotBlank()
                    ) {
                        try {
                            LocalDateTime.of(
                                LocalDate.parse(reminderDate),
                                LocalTime.parse(reminderTime)
                            )
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    val task = Task(
                        id = initialTask?.id ?: System.currentTimeMillis(),
                        title = title.trim(),
                        category = category,
                        priority = priority,
                        dueDate = dueDate.ifBlank { null },
                        reminderAt = reminderAt,
                        completed = initialTask?.completed ?: false,
                        xpAwarded = initialTask?.xpAwarded ?: false,
                        listId = selectedListId
                    )

                    onSave(task)
                },
                enabled = title.isNotBlank() &&
                        validDate &&
                        validReminder
            ) {
                Text(
                    "GUARDAR",
                    color = Purple
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCELAR",
                    color = Gray
                )
            }
        }
    )
}

// COLORES DE CAMPOS

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = White,
        unfocusedTextColor = White,
        focusedBorderColor = Purple,
        unfocusedBorderColor = Gray,
        focusedLabelColor = Purple,
        unfocusedLabelColor = Gray,
        cursorColor = Purple
    )