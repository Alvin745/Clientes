package com.example.countcx

import android.app.AlertDialog
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var listView: ListView
    private lateinit var dataList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        listView = findViewById(R.id.listView)

        // Inicializar la lista de datos
        dataList = mutableListOf()

        // Ejecutar la tarea asincrónica para realizar la solicitud de red
        NetworkTask().execute()

        // Agregar el botón para mostrar el formulario
        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener { showAddDialog() }
    }

    inner class NetworkTask : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String {
            // Realizar la solicitud de red en este método
            return makeHttpRequest()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            // Aquí puedes manejar la respuesta recibida de la solicitud de red
            if (result != null) {
                // Mostrar el Toast de éxito
                showToast("Conexión exitosa con el servidor")

                try {
                    // Parsear los datos JSON
                    val jsonArray = JSONArray(result)

                    // Iterar sobre los objetos JSON y obtener los valores
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val id = jsonObject.getString("id")
                        val nombre = jsonObject.getString("nombre")
                        val edad = jsonObject.getString("edad")
                        val fechaReg = jsonObject.getString("fecha_reg")

                        // Crear una cadena con los valores
                        val item = "ID: $id\nNombre: $nombre\nEdad: $edad\nFecha Registro: $fechaReg"

                        // Agregar la cadena a la lista
                        dataList.add(item)
                    }

                    // Mostrar los datos en el ListView
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, dataList)
                    listView.adapter = adapter

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                // Mostrar el Toast de error
                showToast("Error al conectar con el servidor")
            }
        }
    }

    private fun makeHttpRequest(): String {
        var result = ""
        val url = URL("http://10.0.0.125/androiddbconn/api.php")

        val connection = url.openConnection() as HttpURLConnection
        try {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            result = reader.readText()
            reader.close()
        } finally {
            connection.disconnect()
        }

        return result
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAddDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_add, null)
        builder.setView(dialogView)

        val editTextName = dialogView.findViewById<EditText>(R.id.editTextName)
        val editTextAge = dialogView.findViewById<EditText>(R.id.editTextAge)

        builder.setTitle("Agregar Cliente")
        builder.setPositiveButton("Agregar") { _, _ ->
            val name = editTextName.text.toString().trim()
            val age = editTextAge.text.toString().trim()

            // Validar que los campos no estén vacíos
            if (name.isNotEmpty() && age.isNotEmpty()) {
                // Crear una tarea asincrónica para realizar la solicitud de inserción
                InsertTask().execute(name, age)
            } else {
                showToast("Por favor, ingresa el nombre y la edad")
            }
        }
        builder.setNegativeButton("Cancelar", null)

        val dialog = builder.create()
        dialog.show()
    }

    inner class InsertTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String {
            val name = params[0]
            val age = params[1]

            // Realizar la solicitud de inserción en la base de datos
            return makeHttpPostRequest(name, age)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result != null) {
                // Mostrar el Toast de éxito
                showToast(result)

                dataList.add(result)
                (listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            } else {
                // Mostrar el Toast de error
                showToast("Error al agregar el cliente")
            }
        }
    }

    private fun makeHttpPostRequest(name: String, age: String): String {
        var result = ""
        val url = URL("http://10.0.0.125/androiddbconn/insert.php")

        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true

            val postData = "nombre=${URLEncoder.encode(name, "UTF-8")}&edad=${URLEncoder.encode(age, "UTF-8")}"
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(postData.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            result = reader.readText()
            reader.close()
        } finally {
            connection.disconnect()
        }

        return result
    }
}