
# ReactiveSDK

**ReactiveSDK** — это легковесное решение для авторов android приложений и библиотек, которое предоставляет функциональные реактивные программные интерфейсы (FRP) для работы с асинхронными потоками данных. Библиотека включает в себя основные классы для работы с реактивными потоками, такие как `Single`, `Observable` и `Subjects`.


## Подключение ReactiveSDK к проекту

1. В вашем `<project>/settings.gradle.kts` добавьте репозиторий:
```kotlin
pluginManagement {
    repositories {
        // здесь другие репозитории c вашими зависимостями
        maven { setUrl("https://artifactory-external.vkpartner.ru/artifactory/rustore-maven/") }
    }
}
```

2. В вашем `<project>/<app_module>/build.gradle.kts` добавьте зависимость:
```kotlin
dependencies {
    implementation("ru.rustore:reactive:0.0.1")
}
```

## Основные классы

### Single
Класс `Single` представляет собой реактивный поток, который может завершиться либо успешно, вернув один элемент, либо с ошибкой.

**Пример использования:**
```kotlin
Single.from { "Hello, World!" }
    .subscribe(
        onSuccess = { println(it) },
        onError = { it.printStackTrace() }
    )
```

### Observable
Класс `Observable` представляет собой реактивный поток, который может эмитировать ноль, один или несколько элементов, а также завершиться успешно или с ошибкой.

**Пример использования:**
```kotlin
Observable.create { emitter ->
    emitter.onNext("Hello")
    emitter.onNext("World")
    emitter.onComplete()
}
.subscribe(
    onNext = { println(it) },
    onComplete = { println("Completed") },
    onError = { it.printStackTrace() }
)
```

### Subjects
**Subject** — это горячий тип потока. Это позволяет им одновременно получать данные и распространять их другим подписчикам.
Подписка на горячие источники не может завершиться `onError` или `onComplete`, у подписчиков будет вызываться только `onNext`. 


#### MutableSubject
`MutableSubject` — это изменяемый Subject, который может эмитировать данные и имеет возможность добавлять новых подписчиков.

**Пример использования:**
```kotlin
val subject: MutableSubject<String> = MutableSubject()

subject.observe().subscribe(
    onNext = { println(it) },
    onComplete = { println("Completed") },
    onError = { it.printStackTrace() }
)

subject.emit("Hello")
subject.emit("World")
```

#### MutableStateSubject
`MutableStateSubject` - сохраняет в поле value последнее состояние, поведение очень похоже на `StateFlow` из библиотеки kotlin coroutines.

#### Неизменяемые горячие источники
В библиотеке есть методы преобразования горячих источников в их неизменяемые варианты (`ReadOnlySubject`, `ReadOnlyStateSubject`)

```kotlin
public fun <T> MutableSubject<T>.asSubject(): Subject<T> =
    ReadOnlySubject(this)

public fun <T> MutableStateSubject<T>.asSubject(): Subject<T> =
    ReadOnlySubject(this)

public fun <T> MutableStateSubject<T>.asStateSubject(): StateSubject<T> =
    ReadOnlyStateSubject(this)
```

## Управление потоками
`Single` и `Observable` поддерживают управление потоками с помощью методов `subscribeOn` и `observeOn`.

**Пример использования:**
```kotlin
Observable.create { emitter ->
    emitter.onNext("Hello")
    emitter.onNext("World")
    emitter.onComplete()
}
.subscribeOn(Dispatchers.io) // Все операторы ВЫШЕ будут выполняться на этом потоке
.observeOn(Dispatchers.main) // Все операторы НИЖЕ будут выполняться на этом потоке
.subscribe(
    onNext = { println(it) },
    onComplete = { println("Completed") },
    onError = { it.printStackTrace() }
)
```
