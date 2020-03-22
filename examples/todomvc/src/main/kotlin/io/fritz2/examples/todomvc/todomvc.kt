package io.fritz2.examples.todomvc

import io.fritz2.binding.Const
import io.fritz2.binding.RootStore
import io.fritz2.binding.each
import io.fritz2.binding.eachStore
import io.fritz2.dom.html.HtmlElements
import io.fritz2.dom.html.html
import io.fritz2.dom.mount
import io.fritz2.dom.states
import io.fritz2.dom.values
import io.fritz2.optics.WithId
import io.fritz2.optics.buildLens
import io.fritz2.routing.router
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class ToDo(
    val text: String,
    val completed: Boolean = false,
    override val id: String = text.hashCode().toString()
) : WithId

val textLens = buildLens<ToDo, String>("text", {it.text}, {p,v -> p.copy(text = v)})
val completedLens = buildLens<ToDo, Boolean>("completed", {it.completed}, {p,v -> p.copy(completed = v)})


@ExperimentalCoroutinesApi
@FlowPreview
fun main() {
   // val router = router("/")

    val toDos = object : RootStore<List<ToDo>>(emptyList()) {
        val add = handle<String> { toDos, text -> toDos.plus(ToDo(text)) }

        val remove = handle<ToDo> {toDos, item -> toDos.minus(item) }

        val toggleAll = handle<Boolean> {toDos, toggle ->
            toDos.map { it.copy(completed = toggle)}
        }

        val clearCompleted = handle {toDos ->
            toDos.filter { !it.completed }
        }

        val count = data.map { todos -> todos.count { !it.completed } }.distinctUntilChanged()
        val allChecked = data.map { todos -> todos.all { it.completed }}
    }

    fun HtmlElements.inputHeader() {
        header {
            h1 { +"todos" }
            input {
                className = !"new-todo"
                placeholder = !"What needs to be done?"
                autofocus = Const(true)

                toDos.add <= changes.values().onEach {
                    console.log("adding $it")
                    domNode.value = ""
                }
            }
        }
    }

    fun HtmlElements.mainSection() {
        section {
            className = !"main"
            input("toggle-all") {
                className = !"toggle-all"
                type = !"checkbox"
                checked = toDos.allChecked
                toDos.toggleAll <= changes.states()
                //TODO: set if all items are completed
            }
            label {
                `for` = !"toggle-all"
                +"Mark all as complete"
            }
            ul {
                className = !"todo-list"
                toDos.eachStore().map { toDoStore ->
                    val textStore = toDoStore.sub(textLens)
                    val completedStore = toDoStore.sub(completedLens)
                    html {
                        li {
                            className = completedStore.data.map { if(it) "completed" else "" }
                            div {
                                className = !"view"
                                input {
                                    className = !"toggle"
                                    type = !"checkbox"
                                    checked = completedStore.data
                                    completedStore.update <= changes.states()
                                }
                                label { textStore.data.bind() }
                                button {
                                    className = !"destroy"
                                    toDos.remove <= clicks.events.flatMapLatest { toDoStore.data }
                                }
                            }
                            // <input class="edit" value="Create a TodoMVC template">
                        }
                    }
                }.bind()
            }
        }
    }

    fun HtmlElements.appFooter() {
        footer {
            className = !"footer"
            span {
                className = !"todo-count"
                strong {
                    toDos.count.map {
                        val plural = if (it != 1) "s" else ""
                        "$it item$plural left"
                    }.bind()
                }
            }
            ul {
                className = !"filters"
                //FIMXE: render by loop
                li {
                    a {
                        className = !"selected"
                        href = !"#/"
                        +"All"
                    }
                }
                li {
                    a {
                        href = !"#/active"
                        +"Active"
                    }
                }
                li {
                    a {
                        href = !"#/completed"
                        +"Completed"
                    }
                }
            }
            button {
                className = !"clear-completed"
                +"Clear completed"
                toDos.clearCompleted <= clicks
            }
        }
    }

    val app = html {
        section {
            inputHeader()
            mainSection()
            appFooter()
        }
    }

    app.mount("todoapp")
}