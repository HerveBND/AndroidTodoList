# Spécifications - Application Todo Liste

## 1. Contexte et Objectifs

### Objectif principal
Créer une application todo liste simple servant d'exercice pour la migration de vues XML vers Jetpack Compose, tout en respectant l'architecture actuelle du projet professionnel.

### Langage de développement
- **Kotlin** : Langage principal pour tout le code de l'application

### Architecture cible
- **Navigation/Composants** : Decompose (Arkivanov)
- **Pattern de présentation** : Presenters (pas MVVM pur)
- **Injection de dépendances** : Dagger
- **UI** : Deux versions (XML et Compose) pour pratiquer la migration

## 2. Fonctionnalités

### 2.1 Fonctionnalités principales
- ✅ Ajouter une nouvelle tâche
- ✅ Supprimer une tâche existante (depuis la liste OU depuis le détail)
- ✅ Marquer une tâche comme complétée/non complétée
- ✅ Afficher la liste des tâches
- ✅ Naviguer vers le détail d'une tâche (clic sur la tâche)
- ✅ Afficher le détail d'une tâche avec toutes ses informations

### 2.2 Persistance
- **Format** : Un fichier XML par todo + un fichier d'index XML
- **Nommage** : `<timestamp>.xml` pour les todos, `todos_index.xml` pour l'index
- **Service** : XmlTodoService pour toutes les opérations CRUD sur les fichiers XML
- **Emplacement** :
  - Todos : `filesDir/todos/<timestamp>.xml`
  - Index : `filesDir/todos_index.xml`
- **Stratégie** : Chargement complet au démarrage → cache mémoire dans Repository
- **Synchronisation** : Chaque opération met à jour immédiatement les fichiers XML (simulation push serveur)

### 2.3 États de l'interface
- État de chargement (loading)
- État avec données (liste de tâches)
- État vide (aucune tâche)

## 3. Architecture Technique

### 3.1 Structure des composants Decompose

```
RootComponent (gère la navigation avec ChildStack)
├── TodoListComponent
│   ├── State (data class TodoListState)
│   ├── Presenter (TodoListPresenter)
│   ├── Repository (TodoRepository)
│   ├── onCreateNewTodo() → navigation vers détail (mode création)
│   └── onTodoClick(id: String) → navigation vers détail (mode édition)
│
└── TodoDetailComponent
    ├── todoId: String? (null = mode création, sinon mode édition)
    ├── isCreationMode: Boolean (déduit de todoId == null)
    ├── State (data class TodoDetailState)
    ├── Presenter (TodoDetailPresenter)
    ├── Repository (TodoRepository - partagé)
    ├── onSaveTodo(title, description) → création ou mise à jour
    ├── onDeleteTodo() → suppression + retour navigation
    ├── onToggleTodo() → toggle du statut
    └── onBack() → retour vers liste
```

**Navigation** : Utilisation de `ChildStack` de Decompose pour gérer la pile de navigation
- `Config.List` : Écran liste
- `Config.Detail(todoId: String?)` : Écran détail (null = création, sinon édition)

### 3.2 Couches de l'application

#### Couche UI (2 versions)
- **Version XML** : Fragment + ViewBinding
- **Version Compose** : Composable functions
- Les deux versions utilisent le même Presenter

#### Couche Présentation
- `TodoListPresenter` : Gère la logique métier et l'état
- Expose un `StateFlow<TodoListState>`
- Communique avec le Repository

#### Couche Data
- `TodoRepository` : Interface d'accès aux données (abstraction)
- `TodoRepositoryImpl` : Implémentation utilisant XmlTodoService
- `XmlTodoService` : Service de gestion des opérations CRUD sur le fichier XML
  - Parsing XML → List<Todo>
  - Serialization List<Todo> → XML
  - Opérations : read, write, add, update, delete
- **Pas d'entité distincte** : Le modèle domain `Todo` est directement sérialisé/désérialisé

#### Couche Domain
- `Todo` : Modèle métier (data class)

### 3.3 Injection de dépendances (Dagger)

```
@Component modules:
  - AppModule (Context, filesDir)
  - DataModule (XmlTodoService, TodoRepository)
  - ComponentModule (TodoListComponent.Factory, TodoDetailComponent.Factory)
```

### 3.4 Persistance XML - Structure et Service

#### Architecture multi-fichiers

**Organisation des fichiers** :
```
/data/data/com.example.todolist/files/
├── todos/                              # Dossier contenant tous les fichiers de todos
│   ├── 1705432800000.xml              # Un fichier par todo (nom = timestamp de création)
│   ├── 1705433900000.xml
│   └── ...
└── todos_index.xml                     # Fichier d'index référençant tous les todos
```

#### Structure du fichier d'index (todos_index.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<index>
    <todo-ref>
        <id>550e8400-e29b-41d4-a716-446655440000</id>
        <filename>1705432800000.xml</filename>
        <valid>true</valid>
    </todo-ref>
    <todo-ref>
        <id>6ba7b810-9dad-11d1-80b4-00c04fd430c8</id>
        <filename>1705433900000.xml</filename>
        <valid>false</valid>  <!-- Fichier corrompu, ignoré au chargement -->
    </todo-ref>
</index>
```

**Rôle de l'index** :
- Référencer tous les fichiers de todos existants
- Permettre un chargement rapide sans scanner le système de fichiers
- Marquer les todos invalides (fichiers corrompus détectés)
- Maintenir la liste des UUIDs valides

#### Structure d'un fichier todo individuel (ex: 1705432800000.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<todo>
    <id>550e8400-e29b-41d4-a716-446655440000</id>
    <title>Acheter du lait</title>
    <description>Du lait demi-écrémé bio</description>
    <isCompleted>false</isCompleted>
    <createdAt>1705432800000</createdAt>
</todo>
```

**Caractéristiques** :
- Un fichier = une tâche
- Nom du fichier = timestamp de création (en millisecondes)
- ID = UUID v4 (universally unique identifier)
- Description = champ optionnel (peut être vide)

#### XmlTodoService - Interface

```kotlin
interface XmlTodoService {
    /**
     * Charge tous les todos valides depuis l'index et leurs fichiers respectifs
     * Ignore automatiquement les todos marqués comme invalides dans l'index
     * @return List<Todo> ou liste vide si aucun todo ou si l'index n'existe pas
     */
    suspend fun loadAllTodos(): Result<List<Todo>>

    /**
     * Crée un nouveau todo et son fichier XML
     * - Génère un UUID v4 pour l'ID
     * - Crée le fichier <timestamp>.xml
     * - Met à jour l'index
     * @param title Le titre du todo
     * @param isCompleted L'état initial (généralement false)
     * @return Le todo créé avec son UUID
     */
    suspend fun createTodo(title: String, isCompleted: Boolean = false): Result<Todo>

    /**
     * Met à jour un todo existant
     * - Modifie le fichier XML correspondant
     * - Pas de modification de l'index (sauf si erreur détectée)
     * @param todo Le todo avec les modifications
     */
    suspend fun updateTodo(todo: Todo): Result<Unit>

    /**
     * Supprime un todo
     * - Supprime le fichier XML
     * - Retire l'entrée de l'index
     * @param id L'UUID du todo à supprimer
     */
    suspend fun deleteTodo(id: String): Result<Unit>

    /**
     * Récupère un todo spécifique par son UUID
     * @param id L'UUID du todo recherché
     * @return Le todo ou null si non trouvé ou invalide
     */
    suspend fun getTodoById(id: String): Result<Todo?>

    /**
     * Marque un todo comme invalide dans l'index (en cas de corruption détectée)
     * @param id L'UUID du todo corrompu
     */
    suspend fun markTodoAsInvalid(id: String): Result<Unit>
}
```

#### XmlTodoServiceImpl - Responsabilités

1. **Gestion de l'index** :
   - Parsing de `todos_index.xml` avec `XmlPullParser`
   - Serialization de l'index avec `XmlSerializer`
   - Mise à jour de l'index à chaque création/suppression
   - Marquage des todos invalides

2. **Gestion des fichiers individuels** :
   - Parsing de chaque fichier `<timestamp>.xml` avec `XmlPullParser`
   - Serialization avec `XmlSerializer`
   - Création/suppression de fichiers individuels
   - Nommage basé sur `System.currentTimeMillis()`

3. **Gestion des erreurs** :
   - Catch `IOException`, `XmlPullParserException`
   - En cas de fichier corrompu : log + appel à `markTodoAsInvalid()`
   - Retour de `Result.failure()` avec l'exception

4. **Thread safety** :
   - Utiliser un `Mutex` pour synchroniser tous les accès (index + fichiers)
   - Éviter les race conditions lors de lectures/écritures simultanées

5. **Génération d'UUID** :
   - `UUID.randomUUID()` pour générer des UUID v4
   - Format String : `"550e8400-e29b-41d4-a716-446655440000"`

6. **Initialisation** :
   - Si le dossier `todos/` n'existe pas → le créer
   - Si `todos_index.xml` n'existe pas → créer un index vide

**Points techniques importants** :
- Toutes les opérations sont `suspend` pour compatibilité coroutines
- Utilisation de `Result<T>` pour gestion d'erreurs fonctionnelle
- Un fichier = un todo (pas de fichier monolithique)
- L'index est la source de vérité pour savoir quels fichiers charger

**Workflow de chargement initial** :
```
1. Lire todos_index.xml
2. Pour chaque <todo-ref> avec valid=true :
   - Lire le fichier correspondant
   - Parser le XML → objet Todo
   - Si erreur → marquer comme invalide dans l'index + logger
3. Retourner la liste des todos chargés avec succès
```

**Workflow de création** :
```
1. Générer UUID v4
2. Créer objet Todo(id=UUID, title, isCompleted=false, createdAt=now)
3. Créer fichier <timestamp>.xml avec le todo sérialisé
4. Ajouter une entrée dans l'index (id, filename, valid=true)
5. Sauvegarder l'index
6. Retourner le todo créé
```

## 4. Modèle de données

### Todo (Domain Model)
```kotlin
data class Todo(
    val id: String,              // UUID v4 (ex: "550e8400-e29b-41d4-a716-446655440000")
    val title: String,           // Titre de la tâche
    val description: String,     // Description détaillée (optionnelle)
    val isCompleted: Boolean,    // État de complétion
    val createdAt: Long          // Timestamp de création (millisecondes)
)
```

**Note** : Pas d'entité séparée. Le modèle `Todo` est directement sérialisé/désérialisé en XML.

### TodoListState
```kotlin
data class TodoListState(
    val todos: List<Todo>,
    val isLoading: Boolean,
    val error: String?
)
```

### TodoDetailState
```kotlin
data class TodoDetailState(
    val todo: Todo?,
    val isLoading: Boolean,
    val error: String?
)
```

## 5. User Stories

### US1 : Afficher les tâches
**En tant qu'** utilisateur
**Je veux** voir la liste de mes tâches
**Afin de** connaître ce que j'ai à faire

**Critères d'acceptation** :
- La liste affiche toutes les tâches
- Les tâches complétées sont visuellement différenciées
- Si aucune tâche, afficher un message "Aucune tâche"

### US2 : Ajouter une tâche
**En tant qu'** utilisateur
**Je veux** ajouter une nouvelle tâche
**Afin de** l'ajouter à ma liste

**Critères d'acceptation** :
- Un bouton FAB (+) dans la liste permet d'accéder à l'écran de création
- L'écran de création permet de saisir le titre et la description
- Un bouton "Enregistrer" valide la création
- La tâche apparaît dans la liste après retour
- Retour automatique à la liste après sauvegarde

### US3 : Marquer une tâche comme complétée
**En tant qu'** utilisateur
**Je veux** cocher une tâche
**Afin de** indiquer qu'elle est terminée

**Critères d'acceptation** :
- Chaque tâche a une checkbox
- Cliquer sur la checkbox toggle l'état
- L'état persiste au redémarrage

### US4 : Supprimer une tâche (depuis la liste)
**En tant qu'** utilisateur
**Je veux** supprimer une tâche depuis la liste
**Afin de** nettoyer ma liste rapidement

**Critères d'acceptation** :
- Chaque tâche dans la liste a un bouton de suppression
- La tâche disparaît immédiatement
- La suppression est persistée

### US5 : Naviguer vers le détail d'une tâche
**En tant qu'** utilisateur
**Je veux** cliquer sur une tâche
**Afin de** voir ses détails complets

**Critères d'acceptation** :
- Cliquer sur le texte/zone de la tâche navigue vers le détail
- La transition est fluide
- Le bouton retour permet de revenir à la liste

### US6 : Voir et éditer le détail d'une tâche
**En tant qu'** utilisateur
**Je veux** voir et modifier les détails d'une tâche
**Afin de** consulter et mettre à jour les informations

**Critères d'acceptation** :
- Le titre est affiché et éditable
- La description est affichée et éditable
- L'état (complété/non complété) est visible et modifiable
- La date de création est affichée (non modifiable)
- Un bouton "Enregistrer" permet de sauvegarder les modifications
- Un bouton permet de revenir en arrière

### US7 : Supprimer une tâche (depuis le détail)
**En tant qu'** utilisateur
**Je veux** supprimer une tâche depuis sa vue détail
**Afin de** la supprimer après consultation

**Critères d'acceptation** :
- Un bouton "Supprimer" est présent dans le détail
- Après suppression, retour automatique à la liste
- La tâche n'apparaît plus dans la liste
- La suppression est persistée

## 6. Interface Utilisateur

### Écran principal - TodoListScreen

```
┌─────────────────────────────────┐
│ Ma Todo Liste                   │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ☐ Acheter du lait      [×] │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ ☑ Faire les courses    [×] │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ ☐ Appeler le médecin   [×] │ │
│ └─────────────────────────────┘ │
│                                 │
│                            [+]  │ ← FAB pour créer
└─────────────────────────────────┘
```

**Note** : Le bouton + (FAB) navigue vers l'écran de création.

### Écran création - TodoDetailScreen (mode création)

```
┌─────────────────────────────────┐
│ ← Nouvelle tâche                │
├─────────────────────────────────┤
│                                 │
│  Titre de la tâche              │
│  ┌─────────────────────────┐   │
│  │                         │   │
│  └─────────────────────────┘   │
│                                 │
│  Description (optionnelle)      │
│  ┌─────────────────────────┐   │
│  │                         │   │
│  │                         │   │
│  └─────────────────────────┘   │
│                                 │
│  ┌───────────────────────────┐ │
│  │        ENREGISTRER        │ │
│  └───────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

### Écran détail - TodoDetailScreen (mode édition)

```
┌─────────────────────────────────┐
│ ← Détail de la tâche            │
├─────────────────────────────────┤
│                                 │
│  Titre de la tâche              │
│  ┌─────────────────────────┐   │
│  │ Acheter du lait         │   │
│  └─────────────────────────┘   │
│                                 │
│  Description (optionnelle)      │
│  ┌─────────────────────────┐   │
│  │ Du lait demi-écrémé     │   │
│  └─────────────────────────┘   │
│                                 │
│  Statut                         │
│  ☐ Non complétée                │
│                                 │
│  Créée le                       │
│  16 janvier 2026 à 21:30        │
│                                 │
│  ┌───────────────────────────┐ │
│  │        ENREGISTRER        │ │
│  └───────────────────────────┘ │
│  ┌───────────────────────────┐ │
│  │      SUPPRIMER LA TÂCHE   │ │
│  └───────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

**Note** : Cliquer sur une tâche dans la liste navigue vers cet écran en mode édition.

### Design System
- **Checkbox** : Material Design Checkbox
- **Texte complété** : Barré (strikethrough) + opacité réduite
- **Bouton supprimer** : IconButton avec icône delete (liste) / Button avec texte (détail)
- **Couleurs** : Material Theme par défaut
- **Navigation** : Bouton back dans la TopAppBar

## 7. Navigation avec Decompose

### RootComponent
Point d'entrée de l'application qui gère la navigation avec `ChildStack`.

```kotlin
interface RootComponent {
    val childStack: Value<ChildStack<Config, Child>>

    sealed class Config : Parcelable {
        @Parcelize
        object List : Config()

        @Parcelize
        data class Detail(val todoId: String?) : Config()  // null = mode création
    }

    sealed class Child {
        data class ListChild(val component: TodoListComponent) : Child()
        data class DetailChild(val component: TodoDetailComponent) : Child()
    }
}
```

### TodoListComponent
```kotlin
interface TodoListComponent {
    val state: Value<TodoListState>

    fun onCreateNewTodo()              // Navigation vers détail (création)
    fun onToggleTodo(id: String)
    fun onDeleteTodo(id: String)       // Suppression depuis la liste
    fun onTodoClick(id: String)        // Navigation vers le détail (édition)
    fun onRefresh()
}
```

### TodoDetailComponent
```kotlin
interface TodoDetailComponent {
    val state: Value<TodoDetailState>
    val isCreationMode: Boolean        // true si todoId == null

    fun onSaveTodo(title: String, description: String)  // Création ou mise à jour
    fun onDeleteTodo()                 // Suppression depuis le détail + retour
    fun onBack()                       // Retour vers la liste
    fun onToggleTodo()                 // Toggle depuis le détail
}
```

**Points pédagogiques clés** :
- La propriété `isCreationMode` permet à l'UI de conditionner l'affichage (pas de statut/date en création)
- La fonction `onSaveTodo()` gère à la fois la création et la mise à jour selon le mode
- La fonction `onDeleteTodo()` n'est disponible qu'en mode édition

## 8. Plan de migration XML → Compose

### Phase 1 : Version XML
**Écran Liste** :
1. Créer le layout XML avec RecyclerView
2. Implémenter le Fragment avec ViewBinding
3. Créer l'Adapter pour la RecyclerView
4. Passer les callbacks (onDeleteTodo, onTodoClick) à l'Adapter

**Écran Détail** :
1. Créer le layout XML pour le détail
2. Implémenter le Fragment de détail avec ViewBinding
3. Recevoir le todoId via arguments
4. Configurer les listeners pour les boutons

### Phase 2 : Version Compose
**Écran Liste** :
1. Créer les Composables (TodoListScreen, TodoItem)
2. Utiliser le même Presenter
3. Consommer le StateFlow avec collectAsState()
4. Passer les lambdas directement aux composables

**Écran Détail** :
1. Créer le Composable TodoDetailScreen
2. Utiliser le même Presenter
3. Consommer le StateFlow avec collectAsState()

### Phase 3 : Comparaison et apprentissage
**Points de comparaison** :
- **Passage de callbacks** :
  - XML : Via constructeur d'Adapter + interfaces → verbose
  - Compose : Lambdas directement dans les composables → simple
- **Navigation** :
  - XML : FragmentManager + Bundle pour les arguments
  - Compose : Decompose gère tout de façon type-safe
- **State management** :
  - XML : collect() dans lifecycleScope + update manual de l'UI
  - Compose : collectAsState() + recomposition automatique
- **Boilerplate** :
  - XML : ViewBinding, Adapter, ViewHolder, layouts séparés
  - Compose : Tout en Kotlin, moins de code

### Phase 4 : Focus pédagogique - Passage de la fonction de suppression

#### Dans la version XML (liste)
```
Fragment → Adapter → ViewHolder → View (onClick listener)
         ↓ (interface callback)
         Presenter.onDeleteTodo(id)
```

#### Dans la version Compose (liste)
```
Composable → TodoItem(onDelete = { presenter.onDeleteTodo(id) })
```

#### Dans les deux versions (détail)
Le détail reçoit un composant qui a déjà la logique de suppression + navigation.
C'est le **RootComponent** qui orchestre : suppression → puis pop de la stack.

#### Exemple : Flow de suppression depuis le détail

**Version XML** :
```
1. User clique sur le bouton "SUPPRIMER" (Button dans le layout)
2. Fragment.onDeleteButtonClick() appelé (View.OnClickListener)
3. component.onDeleteTodo() appelé
4. TodoDetailPresenter.onDeleteTodo()
   - Appelle repository.deleteTodo(todoId)
   - Callback vers RootComponent pour navigation.pop()
5. Fragment détruit, retour à la liste
```

**Version Compose** :
```
1. User clique sur le bouton "SUPPRIMER"
2. Lambda onDeleteTodo = component::onDeleteTodo directement appelée
3. TodoDetailPresenter.onDeleteTodo()
   - Appelle repository.deleteTodo(todoId)
   - Callback vers RootComponent pour navigation.pop()
4. Recomposition, écran liste s'affiche
```

**Avantage Compose** : Pas de couche intermédiaire Fragment/Listener, la lambda est passée directement au composable.

## 9. Dépendances Gradle

```kotlin
// Decompose
implementation("com.arkivanov.decompose:decompose:2.2.0")
implementation("com.arkivanov.decompose:extensions-compose-jetpack:2.2.0")

// Dagger
implementation("com.google.dagger:dagger:2.50")
kapt("com.google.dagger:dagger-compiler:2.50")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.01.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

**Note sur la persistance XML** :
- Utilisation de `XmlPullParser` et `XmlSerializer` natifs du SDK Android
- Pas de dépendance externe nécessaire pour la manipulation XML
- Packages : `org.xmlpull.v1.XmlPullParser` et `android.util.Xml`

## 10. Prochaines étapes

1. ✅ Valider ces spécifications
2. ✅ Créer la structure du projet Android
3. ✅ Implémenter la couche Data
   - ✅ XmlTodoService (parsing/serialization XML)
   - ✅ TodoRepository (interface et implémentation)
4. ✅ Implémenter le Presenter et le Component Decompose
5. ✅ Créer la version UI XML (Fragments)
6. ✅ Créer la version UI Compose (Composables)
7. Tester et comparer les deux approches

---

**Note** : Ces spécifications sont volontairement simples pour se concentrer sur l'aspect migration XML/Compose dans un contexte Decompose + Presenter.
