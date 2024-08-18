package com.example.todoapplication

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapplication.RetrofitClient.todoApiService
import java.util.Collections
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    //表示するリストを用意（今は空）
    private var addList = ArrayList<Todo>()

    // RecyclerViewを宣言
    private lateinit var recyclerView: RecyclerView

    // RecyclerViewのAdapterを用意
    private var recyclerAdapter = RecyclerAdapter(addList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ヘッダータイトルを非表示
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        // Viewをセット
        setContentView(R.layout.activity_main)

        // View要素を取得
        val btnAdd: Button = findViewById(R.id.btnAdd)
        recyclerView = findViewById(R.id.rv)

        // コンテンツを変更してもRecyclerViewのレイアウトサイズを変更しない場合はこの設定を使用してパフォーマンスを向上
        recyclerView.setHasFixedSize(true)

        // レイアウトマネージャーで列数を2列に指定
        recyclerView.layoutManager = GridLayoutManager(this, 1, RecyclerView.VERTICAL, false)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        // RecyclerViewにAdapterをセット
        recyclerView.adapter = recyclerAdapter

        // 初期データ取得
        getTodos()

        // 追加ボタン押下時にAlertDialogを表示する
        btnAdd.setOnClickListener {

            // AlertDialog内の表示項目を取得
            val view = layoutInflater.inflate(R.layout.add_todo, null)
            val txtTitle: EditText = view.findViewById(R.id.title)
            val txtPerson: EditText = view.findViewById(R.id.person)

            // AlertDialogを生成
            android.app.AlertDialog.Builder(this)
                // AlertDialogのタイトルを設定
                .setTitle(R.string.addTitle)
                // AlertDialogの表示項目を設定
                .setView(view)
                // AlertDialogのyesボタンを設定し、押下時の挙動を記述
                .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                    // ToDoを生成し、DBに登録しておく
                    //TODO:一旦idをnullで設定。後々適切な値or定数としてまとめたい。
                    val newTodo =
                        Todo(null, txtTitle.text.toString(), txtPerson.text.toString(), false)
                    //TODO:この差分描画だとidがnullのままなので、連続してPUT,DELETEの際にid指定できない。
                    createTodo(newTodo)

                    // 追加差分の描画
                    addList.add(newTodo)
                    recyclerAdapter.notifyItemInserted(addList.size - 1)
                }
                // AlertDialogのnoボタンを設定
                .setNegativeButton(R.string.no, null)
                // AlertDialogを表示
                .show()
        }

        // RecyclerViewにタッチリスナーを追加
        val gestureDetector =
            GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return true
                }
            })

        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val childView: View? = rv.findChildViewUnder(e.x, e.y)
                if (childView != null && gestureDetector.onTouchEvent(e)) {
                    val position = rv.getChildAdapterPosition(childView)
                    if (position != RecyclerView.NO_POSITION) {
                        showEditDialog(position)
                    }
                    return true
                }
                return false
            }
        })

        // 表示しているアイテムがタッチされた時の設定
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                // アイテムをドラッグできる方向を指定
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                // アイテムをスワイプできる方向を指定
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                // アイテムドラッグ時の挙動を設定
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    // リスト内での要素の入れ替え
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    Collections.swap(addList, fromPos, toPos)
                    // UIの更新
                    recyclerAdapter.notifyItemMoved(fromPos, toPos)
                    return true
                }

                // アイテムスワイプ時の挙動を設定
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // アイテムスワイプ時にAlertDialogを表示
                    android.app.AlertDialog.Builder(this@MainActivity)
                        // AlertDialogのタイトルを設定
                        .setTitle(R.string.removeTitle)
                        // AlertDialogのyesボタンを設定
                        .setPositiveButton(R.string.yes) { arg0: DialogInterface, _: Int ->
                            try {
                                // AlertDialogを非表示
                                arg0.dismiss()
                                // UIスレッドで実行
                                runOnUiThread {
                                    val index = viewHolder.adapterPosition
                                    val targetTodo = addList[index]
                                    val targetTodoId = targetTodo.id
                                    // 対象TodoをDBから削除しておく
                                    if (targetTodoId != null) deleteTodo(targetTodoId)

                                    // 削除差分の描画
                                    addList.removeAt(index)
                                    recyclerAdapter.notifyItemRemoved(viewHolder.adapterPosition)
                                }
                            } catch (ignored: Exception) {
                            }
                        }
                        .setNegativeButton(R.string.no) { _: DialogInterface, _: Int ->
                            // 表示するリストを更新(アイテムが変更されたことを通知)
                            recyclerAdapter.notifyItemChanged(viewHolder.adapterPosition)
                        }
                        // AlertDialogを表示
                        .show()
                }
            })

        // 表示しているアイテムがタッチされた時の設定をリストに適用
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // 編集用のダイアログを表示
    private fun showEditDialog(position: Int) {
        val todo = addList[position]

        val view = layoutInflater.inflate(R.layout.add_todo, null)
        val txtTitle: EditText = view.findViewById(R.id.title)
        val txtPerson: EditText = view.findViewById(R.id.person)

        // 既存のデータをセット
        txtTitle.setText(todo.title)
        txtPerson.setText(todo.person)

        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.editTitle)
            .setView(view)
            .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                val updatedTodo = todo.copy(
                    id = todo.id,
                    title = txtTitle.text.toString(),
                    person = txtPerson.text.toString(),
                    done = false
                )
                updateTodo(updatedTodo)
                // 更新差分の描画
                addList[position] = updatedTodo
                recyclerAdapter.notifyItemChanged(position)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    // 全てのTodoを取得する
    private fun getTodos() {
        todoApiService.getTodos().enqueue(object : Callback<TodoResponse> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<TodoResponse>, response: Response<TodoResponse>) {
                if (response.isSuccessful) {
                    // サーバーからのレスポンスを取得
                    val todosResponse = response.body()
                    if (todosResponse != null) {
                        // レスポンスから必要なデータを抽出して整形
                        val todosList = todosResponse.todos.map { todoResponse ->
                            Todo(
                                id = todoResponse.id.value,
                                title = todoResponse.title.value,
                                person = todoResponse.person.value,
                                done = todoResponse.done.value
                            )
                        }

                        // 整形したデータをaddListに設定
                        addList.clear()
                        addList.addAll(todosList)
                        recyclerAdapter.notifyDataSetChanged()

                        Log.d("MainActivity", "Fetched Todos: $addList")
                    } else {
                        Log.e("MainActivity", "Error: ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<TodoResponse>, t: Throwable) {
                Log.e("MainActivity", "Failed to fetch todos", t)
            }
        })
    }

    // 新しいTodoを作成する
    private fun createTodo(newTodo: Todo) {
        todoApiService.createTodo(newTodo).enqueue(object : Callback<Todo> {
            override fun onResponse(call: Call<Todo>, response: Response<Todo>) {
                if (response.isSuccessful) {
                    Log.d("MainActivity", "Todo created: ${response.body()}")
                } else {
                    Log.e("MainActivity", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Todo>, t: Throwable) {
                Log.e("MainActivity", "Failed to create todo", t)
            }
        })
    }

    // Todoを更新する
    private fun updateTodo(updatedTodo: Todo) {
        updatedTodo.id?.let {
            // idがnullでない場合のみ実行される
            todoApiService.updateTodo(it, updatedTodo)
                .enqueue(object : Callback<Todo> {
                    override fun onResponse(call: Call<Todo>, response: Response<Todo>) {
                        if (response.isSuccessful) {
                            // ローカルリストを更新
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Todo updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Log.e("MainActivity", "Error updating todo: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Todo>, t: Throwable) {
                        Log.e("MainActivity", "Failed to update todo", t)
                    }
                })
        }
    }

    // Todoを削除する
    private fun deleteTodo(todoId: Int) {
        todoApiService.deleteTodo(todoId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("MainActivity", "Todo deleted")
                } else {
                    Log.e("MainActivity", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MainActivity", "Failed to delete todo", t)
            }
        })
    }
}


