/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.groups

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.util.ComponentKey
import com.saggitt.colorpickerx.ColorPicker
import com.saggitt.colorpickerx.OnChooseColorListener
import com.saggitt.omega.OmegaPreferencesChangeCallback
import com.saggitt.omega.preferences.SelectableAppsActivity
import com.saggitt.omega.theme.ThemeOverride
import com.saggitt.omega.theme.ThemedContextProvider
import com.saggitt.omega.util.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

abstract class AppGroups<T : AppGroups.Group>(private val manager: AppGroupsManager,
                                              private val type: AppGroupsManager.CategorizationType) {

    private val prefs = manager.prefs
    val context = prefs.context

    private var groupsDataJson by prefs.StringPref(type.prefsKey, "{}", prefs.withChangeCallback {
        onGroupsChanged(it)
    })
    private val groups = ArrayList<T>()

    var isEnabled = manager.categorizationEnabled && manager.categorizationType == type
        private set

    private val defaultGroups by lazy { getDefaultCreators().mapNotNull { it.createGroup(context) } }

    private fun loadGroupsArray(): JSONArray {
        try {
            val obj = JSONObject(groupsDataJson)
            val version = if (obj.has(KEY_VERSION)) obj.getInt(KEY_VERSION) else 0
            if (version > currentVersion) throw IllegalArgumentException("Version $version is higher than supported ($currentVersion)")

            val groups = obj.getJSONArray(KEY_GROUPS)

            // Change the "type" value to string
            if (version < 2) {
                for (i in 0 until groups.length()) {
                    val group = groups.getJSONObject(i)
                    if (group.has(KEY_TYPE)) {
                        group.put(KEY_TYPE, "${group.getInt(KEY_TYPE)}")
                    }
                }
            }

            return groups
        } catch (e: IllegalArgumentException) {
        } catch (e: JSONException) {
        }

        try {
            return JSONArray(groupsDataJson)
        } catch (ignored: JSONException) {
        }

        return JSONArray()
    }

    protected fun loadGroups() {
        groups.clear()
        val arr = loadGroupsArray()
        val used = mutableSetOf<GroupCreator<T>>()
        (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .mapNotNullTo(groups) { group ->
                    val type = if (group.has(KEY_TYPE)) group.getString(KEY_TYPE) else TYPE_UNDEFINED
                    val creator = getGroupCreator(type)
                    used.add(creator)
                    creator.createGroup(context)?.apply { loadCustomizations(context, group.asMap()) }
                }
        getDefaultCreators().asReversed().forEach { creator ->
            if (creator !in used) {
                creator.createGroup(context)?.let { groups.add(0, it) }
            }
        }
    }

    fun checkIsEnabled(changeCallback: OmegaPreferencesChangeCallback) {
        val enabled = manager.categorizationEnabled && manager.categorizationType == type
        if (isEnabled != enabled) {
            isEnabled = enabled
            onGroupsChanged(changeCallback)
        }
    }

    abstract fun getDefaultCreators(): List<GroupCreator<T>>

    abstract fun getGroupCreator(type: String): GroupCreator<T>

    @Suppress("UNUSED_PARAMETER")
    protected fun createNull(context: Context) = null

    abstract fun onGroupsChanged(changeCallback: OmegaPreferencesChangeCallback)

    fun getGroups(): List<T> {
        if (!isEnabled) {
            return defaultGroups
        }
        return groups
    }

    fun setGroups(groups: List<T>) {
        this.groups.clear()
        this.groups.addAll(groups)

        val used = mutableSetOf<GroupCreator<T>>()
        groups.forEach {
            val creator = getGroupCreator(it.type)
            used.add(creator)
        }
        getDefaultCreators().asReversed().forEach { creator ->
            if (creator !in used) {
                creator.createGroup(context)?.let { this.groups.add(0, it) }
            }
        }
    }

    fun saveToJson() {
        val arr = JSONArray()
        groups.forEach { group ->
            arr.put(JSONObject(group.saveCustomizationsInternal(context)))
        }

        val obj = JSONObject()
        obj.put(KEY_VERSION, currentVersion)
        obj.put(KEY_GROUPS, arr)
        groupsDataJson = obj.toString()
    }

    companion object {

        const val currentVersion = 2

        const val KEY_VERSION = "version"
        const val KEY_GROUPS = "tabs"

        const val KEY_ID = "id"
        const val KEY_TYPE = "type"
        const val KEY_COLOR = "color"
        const val KEY_TITLE = "title"
        const val KEY_HIDE_FROM_ALL_APPS = "hideFromAllApps"

        const val TYPE_UNDEFINED = "-1"
    }

    open class Group(val type: String, context: Context, title: String) {

        private val defaultTitle = title

        val customizations = CustomizationMap()
        val title = CustomTitle(KEY_TITLE, defaultTitle)

        init {
            addCustomization(this.title)
        }

        fun getTitle(): String {
            return title.value ?: defaultTitle
        }

        open fun getSummary(context: Context): String? {
            return null
        }

        fun addCustomization(customization: Customization<*, *>) {
            customizations.add(customization)
        }

        open fun loadCustomizations(context: Context, obj: Map<String, Any>) {
            customizations.entries.forEach { it.loadFromJsonInternal(context, obj[it.key]) }
        }

        fun saveCustomizationsInternal(context: Context): Map<String, Any> {
            val obj = HashMap<String, Any>()
            saveCustomizations(context, obj)
            return obj
        }

        open fun saveCustomizations(context: Context, obj: MutableMap<String, Any>) {
            obj[KEY_TYPE] = type
            customizations.entries.forEach { entry ->
                entry.saveToJson(context)?.let { obj[entry.key] = it }
            }
        }

        fun cloneCustomizations(): CustomizationMap {
            return CustomizationMap(customizations)
        }

        abstract class Customization<T : Any, S : Any>(val key: String, protected val default: T) {

            var value: T? = null

            fun value() = value ?: default

            @Suppress("UNCHECKED_CAST")
            fun loadFromJsonInternal(context: Context, obj: Any?) {
                loadFromJson(context, obj as S?)
            }

            abstract fun loadFromJson(context: Context, obj: S?)

            abstract fun saveToJson(context: Context): S?

            abstract fun clone(): Customization<T, S>

            open fun applyFrom(other: Customization<*, *>) {
                value = other.value as? T
            }

            open fun createRow(context: Context, parent: ViewGroup, accent: Int): View? {
                return null
            }
        }

        open class StringCustomization(key: String, default: String) :
                Customization<String, String>(key, default) {

            override fun loadFromJson(context: Context, obj: String?) {
                value = obj
            }

            override fun saveToJson(context: Context): String? {
                return value
            }

            override fun clone(): Customization<String, String> {
                return StringCustomization(key, default).also { it.value = value }
            }
        }

        class CustomTitle(key: String, default: String) : StringCustomization(key, default) {

            override fun createRow(context: Context, parent: ViewGroup, accent: Int): View? {
                val view = LayoutInflater.from(context).inflate(R.layout.drawer_tab_custom_title_row, parent, false)
                view.findViewById<TextView>(R.id.name_label).setTextColor(accent)

                val tabName = view.findViewById<TextView>(R.id.name)
                tabName.text = value
                tabName.hint = default
                tabName.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        value = s?.toString()?.trim()?.asNonEmpty()
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    }

                })
                return view
            }

            override fun saveToJson(context: Context): String? {
                return value?.asNonEmpty()
            }

            override fun clone(): Customization<String, String> {
                return CustomTitle(key, default).also { it.value = value }
            }
        }

        open class BooleanCustomization(key: String, default: Boolean) :
                Customization<Boolean, Boolean>(key, default) {

            override fun loadFromJson(context: Context, obj: Boolean?) {
                value = obj
            }

            override fun saveToJson(context: Context): Boolean? {
                return value
            }

            override fun clone(): Customization<Boolean, Boolean> {
                return BooleanCustomization(key, default).also { it.value = value }
            }
        }

        open class LongCustomization(key: String, default: Long) : Customization<Long, Long>(key, default) {
            override fun loadFromJson(context: Context, obj: Long?) {
                value = obj
            }

            override fun saveToJson(context: Context): Long? {
                return value
            }

            override fun clone(): Customization<Long, Long> {
                return LongCustomization(key, default).also { it.value = value }
            }

        }

        class SwitchRow(private val icon: Int, private val label: Int, key: String, default: Boolean) :
                BooleanCustomization(key, default) {

            override fun createRow(context: Context, parent: ViewGroup, accent: Int): View? {
                val view = LayoutInflater.from(context).inflate(R.layout.drawer_tab_switch_row, parent, false)

                view.findViewById<ImageView>(R.id.icon).apply {
                    setImageResource(icon)
                    tintDrawable(accent)
                }

                view.findViewById<TextView>(R.id.title).setText(label)

                val switch = view.findViewById<Switch>(R.id.switch_widget)
                switch.isChecked = value()
                switch.applyColor(accent)

                view.setOnClickListener {
                    value = !value()
                    switch.isChecked = value()
                }

                return view
            }

            override fun clone(): Customization<Boolean, Boolean> {
                return SwitchRow(icon, label, key, default).also { it.value = value }
            }
        }

        open class ColorCustomization(key: String, default: Int) : Customization<Int, String>(key, default) {
            override fun loadFromJson(context: Context, obj: String?) {
                value = obj?.let { AppGroupsUtils.getInstance(context).getTabColor(it) }
            }

            override fun saveToJson(context: Context): String? {
                return value.toString()
            }

            override fun clone(): Customization<Int, String> {
                return ColorCustomization(key, default).also { it.value = value }
            }
        }

        class ColorRow(key: String, default: Int) : ColorCustomization(key, default) {
            override fun createRow(context: Context, parent: ViewGroup, accent: Int): View? {
                val view = LayoutInflater.from(context).inflate(R.layout.drawer_tab_color_row, parent, false)

                updateColor(view)

                val themedContext = ThemedContextProvider(context, null, ThemeOverride.Settings()).get()

                view.setOnClickListener {
                    val colorPicker = ColorPicker(themedContext)
                    colorPicker.setTitle(context.getString(R.string.tab_color));
                    colorPicker.setRoundColorButton(true)
                    colorPicker.show()
                    colorPicker.setOnChooseColorListener(object : OnChooseColorListener {
                        override fun onCancel() {
                            colorPicker.dismissDialog()
                        }

                        override fun onChooseColor(position: Int, color: Int) {
                            value = color;
                            updateColor(view)
                        }
                    })
                }

                return view
            }

            private fun updateColor(view: View) {
                view.findViewById<ImageView>(R.id.color_ring_icon).tintDrawable(value())
            }

            override fun clone(): Customization<Int, String> {
                return ColorRow(key, default).also { it.value = value }
            }
        }

        abstract class SetCustomization<T : Any, S : Any>(key: String, default: MutableSet<T>) :
                Customization<MutableSet<T>, JSONArray>(key, default) {

            @Suppress("UNCHECKED_CAST")
            override fun loadFromJson(context: Context, obj: JSONArray?) {
                if (obj == null) {
                    value = null
                } else {
                    val set = HashSet<T>()
                    for (i in (0 until obj.length())) {
                        set.add(unflatten(context, obj.get(i) as S))
                    }
                    value = set
                }
            }

            override fun saveToJson(context: Context): JSONArray? {
                val list = value ?: return null
                val array = JSONArray()
                list.forEach { array.put(flatten(it)) }
                return array
            }

            abstract fun unflatten(context: Context, value: S): T

            abstract fun flatten(value: T): S
        }

        open class ComponentsCustomization(key: String, default: MutableSet<ComponentKey>) :
                SetCustomization<ComponentKey, String>(key, default) {

            override fun loadFromJson(context: Context, obj: JSONArray?) {
                super.loadFromJson(context, obj)
                if (value == null) {
                    value = HashSet(default)
                }
            }

            override fun unflatten(context: Context, value: String): ComponentKey {
                return Utilities.makeComponentKey(context, value)
            }

            override fun flatten(value: ComponentKey): String {
                return value.toString()
            }

            override fun clone(): Customization<MutableSet<ComponentKey>, JSONArray> {
                return ComponentsCustomization(key, default).also { newInstance ->
                    value?.let { newInstance.value = HashSet(it) }
                }
            }
        }

        class AppsRow(key: String, default: MutableSet<ComponentKey>) :
                ComponentsCustomization(key, default) {

            override fun createRow(context: Context, parent: ViewGroup, accent: Int): View? {
                val view = LayoutInflater.from(context).inflate(R.layout.drawer_tab_apps_row, parent, false)

                view.findViewById<ImageView>(R.id.manage_apps_icon).tintDrawable(accent)
                updateCount(view)

                view.setOnClickListener {
                    SelectableAppsActivity.start(context, value(), { newSelections ->
                        if (newSelections != null) {
                            value = HashSet(newSelections)
                            updateCount(view)
                        }
                    }, DrawerTabs.Profile())
                }

                return view
            }

            private fun updateCount(view: View) {
                val count = value().size
                view.findViewById<TextView>(R.id.apps_count).text =
                        view.resources.getQuantityString(R.plurals.tab_apps_count, count, count)
            }

            override fun clone(): Customization<MutableSet<ComponentKey>, JSONArray> {
                return AppsRow(key, default).also { newInstance ->
                    value?.let { newInstance.value = HashSet(it) }
                }
            }
        }

        class CustomizationMap(old: CustomizationMap? = null) {

            private val map = HashMap<String, Customization<*, *>>()
            private val order = HashMap<String, Int>()

            init {
                old?.map?.mapValuesTo(map) { it.value.clone() }
                old?.order?.entries?.forEach { order[it.key] = it.value }
            }

            fun add(customization: Customization<*, *>) {
                map[customization.key] = customization
            }

            fun get(customization: Customization<*, *>): Customization<*, *>? {
                return map[customization.key]
            }

            fun setOrder(vararg keys: String) {
                keys.forEachIndexed { index, s -> order[s] = index }
            }

            fun applyFrom(config: CustomizationMap) {
                map.values.forEach { entry ->
                    val other = config.map[entry.key] ?: return@forEach
                    entry.applyFrom(other)
                }
            }

            val entries get() = map.values

            val sortedEntries
                get() =
                    if (order.isEmpty()) entries
                    else entries.sortedBy { order[it.key] }

            override fun equals(other: Any?): Boolean {
                if (other !is CustomizationMap) return false
                return map == other.map
            }

            override fun hashCode(): Int {
                return map.hashCode()
            }
        }
    }
}

class AppGroupsUtils(context: Context) {

    val defaultColor = Utilities.getOmegaPrefs(context).accentColor

    fun getTabColor(color: String): Int {
        Log.d("AppGroupsUtils", "Loading tab color for $color")
        return if (color != "null") {
            color.toInt();
        } else {
            defaultColor
        }
    }

    companion object : SingletonHolder<AppGroupsUtils, Context>(
            ensureOnMainThread(useApplicationContext(::AppGroupsUtils)))
}