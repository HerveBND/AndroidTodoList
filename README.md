# TodoList - Application Android Multi-Architecture

Application de gestion de tâches démontrant deux approches d'architecture UI sur Android avec une couche métier partagée utilisant **Decompose** pour la navigation et la gestion d'état.

## Table des matières

- [Architecture Globale](#architecture-globale)
- [Architecture XML/Fragments](#architecture-xmlfragments)
- [Architecture Jetpack Compose](#architecture-jetpack-compose)
- [Flux d'Initialisation d'État](#flux-dinitialisation-détat)
- [Diagrammes de Classes](#diagrammes-de-classes)
- [Basculer entre les Architectures](#basculer-entre-les-architectures)
- [Structure du Projet](#structure-du-projet)
- [Technologies Utilisées](#technologies-utilisées)

---

## Architecture Globale

L'application utilise une architecture en couches avec séparation claire des responsabilités :

```
┌───────────────────────────────────────────────────────────┐
│                        UI Layer                           │
│  ┌───────────────────────┐  ┌───────────────────────────┐ │
│  │   XML/Fragments       │  │   Jetpack Compose         │ │
│  │   - MainActivity      │  │   - ComposeActivity       │ │
│  │   - TodoListFragment  │  │   - TodoListScreen        │ │
│  │   - TodoDetailFrag.   │  │   - TodoDetailScreen      │ │
│  └───────────────────────┘  └───────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                   Presentation Layer                      │
│  ┌───────────────────────────────────────────────────────┐│
│  │                 Decompose Components                  ││
│  │   - RootComponent (Navigation Stack)                  ││
│  │   - TodoListComponent + Presenter                     ││
│  │   - TodoDetailComponent + Presenter                   ││
│  └───────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                      Domain Layer                         │
│  ┌───────────────────────────────────────────────────────┐│
│  │   - Todo (Model)                                      ││
│  │   - TodoRepository (Interface)                        ││
│  └───────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                       Data Layer                          │
│  ┌───────────────────────────────────────────────────────┐│
│  │   - TodoRepositoryImpl                                ││
│  │   - XmlTodoService (Persistance XML)                  ││
│  └───────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────┘
```

---

## Architecture XML/Fragments

### Composants Principaux

| Composant | Responsabilité |
|-----------|----------------|
| `MainActivity` | Héberge les Fragments, observe la ChildStack Decompose, gère le back |
| `TodoListFragment` | Affiche la liste avec RecyclerView, observe le state du Component |
| `TodoDetailFragment` | Formulaire d'édition/création, gère l'état local des champs |
| `TodoAdapter` | Adapter RecyclerView avec DiffUtil |

### Flux de Données

```mermaid
sequenceDiagram
    participant MA as MainActivity
    participant F as Fragment
    participant C as Component
    participant P as Presenter
    participant R as Repository

    MA->>C: Observe childStack
    C-->>MA: Child (ListChild/DetailChild)
    MA->>F: showFragment(component)
    F->>C: setComponent(component)
    F->>C: state.subscribe(observer)
    C-->>F: State initial
    F->>F: Mise à jour UI

    Note over F,R: Action utilisateur (Save)
    F->>C: onSaveTodo(title, desc, completed)
    C->>P: onSaveTodo(...)
    P->>R: updateTodo(todo)
    R-->>P: Result
    P->>P: onTodoUpdated callback
    P-->>C: Navigation back
```

### Gestion du Back Button (XML)

```mermaid
flowchart TD
    A[Back Pressed] --> B{Backstack Decompose vide?}
    B -->|Non| C[Callback désactivé]
    C --> D[Decompose gère pop]
    D --> E[Nouveau Fragment affiché]
    E --> F[État local perdu - non persisté]
    F --> G[State rechargé depuis Repository]
    B -->|Oui| H[Callback activé]
    H --> I[Activity.finish]
```

### Gestion de l'État Local (XML)

```kotlin
// Fragment - État local pour les champs éditables
private var isInitialLoad = true

// Dans observeState()
if (isInitialLoad) {
    binding.titleEditText.setText(todo.title)
    binding.descriptionEditText.setText(todo.description)
    binding.statusCheckBox.isChecked = todo.isCompleted
    isInitialLoad = false
}
// Les modifications ne sont persistées qu'au clic sur Save
```

---

## Architecture Jetpack Compose

### Composants Principaux

| Composant | Responsabilité |
|-----------|----------------|
| `ComposeActivity` | Point d'entrée, initialise RootComponent |
| `RootContent` | Observe childStack, affiche le bon Screen |
| `TodoListScreen` | Composable liste avec LazyColumn |
| `TodoDetailScreen` | Composable formulaire avec état local (remember) |

### Flux de Données

```mermaid
sequenceDiagram
    participant CA as ComposeActivity
    participant RC as RootContent
    participant S as Screen
    participant C as Component
    participant P as Presenter

    CA->>RC: RootContent(rootComponent)
    RC->>C: childStack.subscribeAsState()
    C-->>RC: ChildStack
    RC->>S: TodoListScreen/TodoDetailScreen
    S->>C: state.subscribeAsState()
    C-->>S: State
    S->>S: Recomposition UI

    Note over S,P: Action utilisateur (Save)
    S->>C: onSaveTodo(title, desc, completed)
    C->>P: Délègue au Presenter
    P-->>C: Callback navigation
```

### Gestion de l'État Local (Compose)

```kotlin
// États locaux pour les champs éditables
var title by remember(state.todo?.title) {
    mutableStateOf(state.todo?.title ?: "")
}
var description by remember(state.todo?.description) {
    mutableStateOf(state.todo?.description ?: "")
}
var isCompleted by remember(state.todo?.isCompleted) {
    mutableStateOf(state.todo?.isCompleted ?: false)
}

// Sauvegarde uniquement au clic sur Save
Button(onClick = { component.onSaveTodo(title, description, isCompleted) })
```

L'état local est initialisé depuis le Repository via `remember(key)`. Les modifications ne sont persistées qu'au clic sur Save. Le back button abandonne les modifications.

---

## Flux d'Initialisation d'État

### Démarrage de l'Application

```mermaid
sequenceDiagram
    participant App as TodoApplication
    participant DI as Dagger/AppComponent
    participant Act as Activity
    participant RC as RootComponent
    participant LC as ListComponent
    participant P as Presenter
    participant R as Repository
    participant X as XmlTodoService

    App->>DI: Initialisation DI
    DI->>X: Création XmlTodoService (Singleton)
    DI->>R: Création TodoRepository (Singleton)

    Act->>RC: DefaultRootComponent(repository)
    RC->>LC: createChild(Config.List)
    LC->>P: TodoListPresenter(repository, scope)

    P->>R: repository.todos.collect()
    P->>R: loadTodos()
    R->>X: loadAllTodos()
    X-->>R: List<Todo>
    R->>R: _todos.value = list
    R-->>P: Flow émet la liste
    P->>P: _state.update { isLoading = false }
```

### Navigation vers Détail

```mermaid
sequenceDiagram
    participant UI as UI (Fragment/Screen)
    participant LC as ListComponent
    participant RC as RootComponent
    participant DC as DetailComponent
    participant P as DetailPresenter
    participant R as Repository

    UI->>LC: onTodoClick(todoId)
    LC->>RC: onNavigateToDetail(todoId)
    RC->>RC: navigation.push(Config.Detail(todoId))
    RC->>DC: createChild(Config.Detail)
    DC->>P: TodoDetailPresenter(todoId, repository)

    P->>R: repository.todos.collect()
    P->>R: getTodoById(todoId)
    R-->>P: Todo
    P->>P: _state.update { todo = it }

    RC-->>UI: childStack émet DetailChild
    UI->>UI: Affiche écran détail
```

### Sauvegarde et Retour

```mermaid
sequenceDiagram
    participant UI as UI (Fragment/Screen)
    participant C as DetailComponent
    participant P as Presenter
    participant R as Repository
    participant RC as RootComponent

    UI->>C: onSaveTodo(title, desc, isCompleted)
    C->>P: onSaveTodo(...)

    alt Mode Création
        P->>R: addTodo(title, description)
        R-->>P: Result<Todo>
        P->>P: onTodoCreated callback
    else Mode Édition
        P->>R: updateTodo(todo)
        R-->>P: Result<Unit>
        P->>P: onTodoUpdated callback
    end

    P-->>C: Callback déclenché
    C->>RC: onNavigateBack()
    RC->>RC: navigation.pop()
    RC-->>UI: childStack émet ListChild
```

---

## Diagrammes de Classes

### Couche Presentation (Decompose)

```mermaid
classDiagram
    class RootComponent {
        <<interface>>
        +childStack: Value~ChildStack~
    }

    class DefaultRootComponent {
        -navigation: StackNavigation
        -repository: TodoRepository
        +childStack: Value~ChildStack~
        -createChild(config, context): Child
    }

    class TodoListComponent {
        <<interface>>
        +state: Value~TodoListState~
        +onTodoClick(id: String)
        +onCreateNewTodo()
        +onToggleTodo(id: String)
        +onDeleteTodo(id: String)
    }

    class TodoDetailComponent {
        <<interface>>
        +state: Value~TodoDetailState~
        +isCreationMode: Boolean
        +onSaveTodo(title, description, isCompleted)
        +onDeleteTodo()
        +onBack()
    }

    class TodoListPresenter {
        -repository: TodoRepository
        -scope: CoroutineScope
        +state: StateFlow~TodoListState~
        +loadTodos()
        +onToggleTodo(id: String)
        +onDeleteTodo(id: String)
    }

    class TodoDetailPresenter {
        -todoId: String?
        -repository: TodoRepository
        -onTodoCreated: Callback
        -onTodoUpdated: Callback
        -onTodoDeleted: Callback
        +state: StateFlow~TodoDetailState~
        +isCreationMode: Boolean
        +onSaveTodo(title, description, isCompleted)
        +onDeleteTodo()
    }

    RootComponent <|.. DefaultRootComponent
    TodoListComponent <|.. DefaultTodoListComponent
    TodoDetailComponent <|.. DefaultTodoDetailComponent
    DefaultTodoListComponent --> TodoListPresenter
    DefaultTodoDetailComponent --> TodoDetailPresenter
    DefaultRootComponent --> TodoListComponent
    DefaultRootComponent --> TodoDetailComponent
```

### Couche Data

```mermaid
classDiagram
    class Todo {
        +id: String
        +title: String
        +description: String
        +isCompleted: Boolean
        +createdAt: Long
    }

    class TodoRepository {
        <<interface>>
        +todos: Flow~List~Todo~~
        +loadTodos(): Result~Unit~
        +addTodo(title, description): Result~Todo~
        +updateTodo(todo): Result~Unit~
        +toggleTodoCompletion(id): Result~Unit~
        +deleteTodo(id): Result~Unit~
        +getTodoById(id): Result~Todo?~
    }

    class TodoRepositoryImpl {
        -xmlTodoService: XmlTodoService
        -_todos: MutableStateFlow
        +todos: Flow~List~Todo~~
    }

    class XmlTodoService {
        <<interface>>
        +loadAllTodos(): Result~List~Todo~~
        +createTodo(title, desc, completed): Result~Todo~
        +updateTodo(todo): Result~Unit~
        +deleteTodo(id): Result~Unit~
        +getTodoById(id): Result~Todo?~
    }

    class XmlTodoServiceImpl {
        -context: Context
        -mutex: Mutex
        -parseXml()
        -writeXml()
    }

    TodoRepository <|.. TodoRepositoryImpl
    XmlTodoService <|.. XmlTodoServiceImpl
    TodoRepositoryImpl --> XmlTodoService
    TodoRepositoryImpl --> Todo
```

### Couche UI - XML/Fragments

```mermaid
classDiagram
    class MainActivity {
        -binding: ActivityMainBinding
        -rootComponent: RootComponent
        -backCallback: OnBackPressedCallback
        -observeChildStack()
        -showListFragment(component)
        -showDetailFragment(component)
        -setupBackPressedHandler()
    }

    class TodoListFragment {
        -binding: FragmentTodoListBinding
        -component: TodoListComponent
        -adapter: TodoAdapter
        -stateObserver: Function
        +setComponent(component)
        -observeState()
    }

    class TodoDetailFragment {
        -binding: FragmentTodoDetailBinding
        -component: TodoDetailComponent
        -isInitialLoad: Boolean
        -stateObserver: Function
        +setComponent(component)
        -setupListeners()
        -observeState()
    }

    class TodoAdapter {
        -onTodoClick: Function
        -onToggleClick: Function
        -onDeleteClick: Function
        +onBindViewHolder()
    }

    MainActivity --> TodoListFragment
    MainActivity --> TodoDetailFragment
    TodoListFragment --> TodoAdapter
    TodoListFragment --> TodoListComponent
    TodoDetailFragment --> TodoDetailComponent
```

### Couche UI - Jetpack Compose

```mermaid
classDiagram
    class ComposeActivity {
        -rootComponent: RootComponent
        +onCreate()
    }

    class RootContent {
        <<Composable>>
        +component: RootComponent
        -childStack: State
    }

    class TodoListScreen {
        <<Composable>>
        +component: TodoListComponent
        -state: State
    }

    class TodoDetailScreen {
        <<Composable>>
        +component: TodoDetailComponent
        -title: MutableState
        -description: MutableState
        -isCompleted: MutableState
    }

    ComposeActivity --> RootContent
    RootContent --> TodoListScreen
    RootContent --> TodoDetailScreen
    TodoListScreen --> TodoListComponent
    TodoDetailScreen --> TodoDetailComponent
```

---

## Basculer entre les Architectures

### Configuration Actuelle : XML/Fragments (par défaut)

L'application est actuellement configurée pour utiliser les **vues XML**.

### Pour utiliser Jetpack Compose

Modifier le fichier `app/src/main/AndroidManifest.xml` :

**Étape 1 : Retirer l'intent-filter de MainActivity**

```xml
<!-- AVANT -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.TodoList">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".ComposeActivity"
    android:exported="true"
    android:theme="@style/Theme.TodoList">
</activity>
```

**Étape 2 : Ajouter l'intent-filter à ComposeActivity**

```xml
<!-- APRÈS -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.TodoList">
</activity>

<activity
    android:name=".ComposeActivity"
    android:exported="true"
    android:theme="@style/Theme.TodoList">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**Résumé : Déplacer le bloc `<intent-filter>` de `MainActivity` vers `ComposeActivity`.**

---

## Structure du Projet

```
app/src/main/java/com/example/todolist/
├── TodoApplication.kt              # Application + DI
├── MainActivity.kt                 # Activity XML/Fragments
├── ComposeActivity.kt              # Activity Compose
│
├── di/                             # Injection de dépendances (Dagger)
│   └── AppComponent.kt
│
├── domain/
│   └── model/
│       └── Todo.kt                 # Modèle de données
│
├── data/
│   ├── local/
│   │   ├── XmlTodoService.kt       # Interface persistance
│   │   └── XmlTodoServiceImpl.kt   # Implémentation XML
│   └── repository/
│       ├── TodoRepository.kt       # Interface repository
│       └── TodoRepositoryImpl.kt   # Implémentation
│
├── presentation/
│   ├── TodoListState.kt            # État écran liste
│   ├── TodoDetailState.kt          # État écran détail
│   ├── component/
│   │   ├── RootComponent.kt        # Navigation Decompose
│   │   ├── TodoListComponent.kt    # Component liste
│   │   └── TodoDetailComponent.kt  # Component détail
│   └── presenter/
│       ├── TodoListPresenter.kt    # Logique liste
│       └── TodoDetailPresenter.kt  # Logique détail
│
└── ui/
    ├── xml/                        # UI XML/Fragments
    │   ├── TodoListFragment.kt
    │   ├── TodoDetailFragment.kt
    │   └── TodoAdapter.kt
    └── compose/                    # UI Jetpack Compose
        ├── RootContent.kt
        ├── TodoListScreen.kt
        └── TodoDetailScreen.kt
```

---

## Fonctionnalités

- Afficher la liste des tâches
- Ajouter une nouvelle tâche (via écran de création)
- Modifier une tâche existante (titre, description, statut)
- Marquer une tâche comme complétée/non complétée
- Supprimer une tâche (depuis la liste OU depuis le détail)
- Persistance locale en XML
- Annulation des modifications via bouton retour

---

## Technologies Utilisées

| Technologie | Version | Usage |
|-------------|---------|-------|
| Kotlin | 1.9.x | Langage |
| Decompose | 2.2.0 | Navigation & State Management |
| Dagger | 2.50 | Injection de dépendances |
| Jetpack Compose | BOM 2024.06.00 | UI déclarative |
| ViewBinding | - | UI XML |
| Coroutines | 1.7.3 | Asynchrone |
| Material 3 | 1.11.0 | Design System |

---

## Build & Run

```bash
# Build
./gradlew assembleDebug

# Install
./gradlew installDebug

# Tests
./gradlew test
```

---

## Documentation Complémentaire

- [SPECS.md](SPECS.md) - Spécifications détaillées
- [.claude/decompose-notes.md](.claude/decompose-notes.md) - Notes sur l'API Decompose

---

## Licence

MIT License
