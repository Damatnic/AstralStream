package com.astralplayer.nextplayer.feature.localization

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

// DataStore extension
private val Context.localizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "localization")

/**
 * Data class representing a supported language
 */
data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val isRTL: Boolean = false,
    val completionPercentage: Int = 100
)

/**
 * Data class for localized strings
 */
data class LocalizedStrings(
    // Player Controls
    val play: String,
    val pause: String,
    val stop: String,
    val next: String,
    val previous: String,
    val seekForward: String,
    val seekBackward: String,
    val fullscreen: String,
    val exitFullscreen: String,
    val volume: String,
    val mute: String,
    val unmute: String,
    val settings: String,
    val subtitles: String,
    val audioTracks: String,
    val playbackSpeed: String,
    val quality: String,
    
    // File Browser
    val browse: String,
    val folder: String,
    val file: String,
    val videos: String,
    val audio: String,
    val images: String,
    val documents: String,
    val recent: String,
    val favorites: String,
    val search: String,
    val sortBy: String,
    val name: String,
    val size: String,
    val date: String,
    val type: String,
    
    // Streaming
    val liveStreaming: String,
    val stream: String,
    val streaming: String,
    val viewers: String,
    val bitrate: String,
    val fps: String,
    val startStream: String,
    val stopStream: String,
    val streamKey: String,
    val streamUrl: String,
    
    // Cast
    val cast: String,
    val casting: String,
    val castTo: String,
    val connected: String,
    val connecting: String,
    val disconnect: String,
    val noDevicesFound: String,
    val castingTo: String,
    
    // Bookmarks & Chapters
    val bookmarks: String,
    val bookmark: String,
    val addBookmark: String,
    val chapters: String,
    val chapter: String,
    val addChapter: String,
    val notes: String,
    val tags: String,
    
    // Settings
    val language: String,
    val theme: String,
    val appearance: String,
    val playback: String,
    val network: String,
    val storage: String,
    val privacy: String,
    val about: String,
    val version: String,
    val developer: String,
    
    // Common
    val ok: String,
    val cancel: String,
    val save: String,
    val delete: String,
    val edit: String,
    val add: String,
    val remove: String,
    val close: String,
    val back: String,
    val next_: String,
    val previous_: String,
    val loading: String,
    val error: String,
    val retry: String,
    val success: String,
    val warning: String,
    val info: String,
    
    // Time & Duration
    val duration: String,
    val position: String,
    val remaining: String,
    val elapsed: String,
    val hours: String,
    val minutes: String,
    val seconds: String,
    
    // Quality & Format
    val resolution: String,
    val format: String,
    val codec: String,
    val frameRate: String,
    val aspectRatio: String,
    val colorSpace: String,
    
    // Notifications
    val nowPlaying: String,
    val paused: String,
    val stopped: String,
    val buffering: String,
    val mediaLoaded: String,
    val mediaError: String
)

/**
 * Manager for localization and internationalization
 */
class LocalizationManager(private val context: Context) {
    
    private object Keys {
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val USE_SYSTEM_LANGUAGE = booleanPreferencesKey("use_system_language")
    }
    
    // Supported languages
    private val supportedLanguages = listOf(
        SupportedLanguage("en", "English", "English", "🇺🇸", false, 100),
        SupportedLanguage("es", "Spanish", "Español", "🇪🇸", false, 95),
        SupportedLanguage("fr", "French", "Français", "🇫🇷", false, 90),
        SupportedLanguage("de", "German", "Deutsch", "🇩🇪", false, 88),
        SupportedLanguage("it", "Italian", "Italiano", "🇮🇹", false, 85),
        SupportedLanguage("pt", "Portuguese", "Português", "🇵🇹", false, 82),
        SupportedLanguage("ru", "Russian", "Русский", "🇷🇺", false, 80),
        SupportedLanguage("zh", "Chinese", "中文", "🇨🇳", false, 78),
        SupportedLanguage("ja", "Japanese", "日本語", "🇯🇵", false, 75),
        SupportedLanguage("ko", "Korean", "한국어", "🇰🇷", false, 72),
        SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦", true, 70),
        SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳", false, 68),
        SupportedLanguage("tr", "Turkish", "Türkçe", "🇹🇷", false, 65),
        SupportedLanguage("pl", "Polish", "Polski", "🇵🇱", false, 62),
        SupportedLanguage("nl", "Dutch", "Nederlands", "🇳🇱", false, 60)
    )
    
    // Current language flow
    val currentLanguage: Flow<SupportedLanguage> = context.localizationDataStore.data
        .map { preferences ->
            val useSystemLanguage = preferences[Keys.USE_SYSTEM_LANGUAGE] ?: true
            val selectedLanguageCode = preferences[Keys.SELECTED_LANGUAGE] ?: "en"
            
            if (useSystemLanguage) {
                val systemLanguage = Locale.getDefault().language
                supportedLanguages.find { it.code == systemLanguage } 
                    ?: supportedLanguages.first { it.code == "en" }
            } else {
                supportedLanguages.find { it.code == selectedLanguageCode }
                    ?: supportedLanguages.first { it.code == "en" }
            }
        }
    
    // Use system language setting
    val useSystemLanguage: Flow<Boolean> = context.localizationDataStore.data
        .map { preferences ->
            preferences[Keys.USE_SYSTEM_LANGUAGE] ?: true
        }
    
    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): List<SupportedLanguage> = supportedLanguages
    
    /**
     * Set selected language
     */
    suspend fun setLanguage(languageCode: String) {
        context.localizationDataStore.edit { preferences ->
            preferences[Keys.SELECTED_LANGUAGE] = languageCode
            preferences[Keys.USE_SYSTEM_LANGUAGE] = false
        }
    }
    
    /**
     * Enable/disable system language
     */
    suspend fun setUseSystemLanguage(useSystem: Boolean) {
        context.localizationDataStore.edit { preferences ->
            preferences[Keys.USE_SYSTEM_LANGUAGE] = useSystem
        }
    }
    
    /**
     * Get localized strings for current language
     */
    suspend fun getLocalizedStrings(language: SupportedLanguage): LocalizedStrings {
        return when (language.code) {
            "es" -> getSpanishStrings()
            "fr" -> getFrenchStrings()
            "de" -> getGermanStrings()
            "it" -> getItalianStrings()
            "pt" -> getPortugueseStrings()
            "ru" -> getRussianStrings()
            "zh" -> getChineseStrings()
            "ja" -> getJapaneseStrings()
            "ko" -> getKoreanStrings()
            "ar" -> getArabicStrings()
            "hi" -> getHindiStrings()
            "tr" -> getTurkishStrings()
            "pl" -> getPolishStrings()
            "nl" -> getDutchStrings()
            else -> getEnglishStrings() // Default to English
        }
    }
    
    /**
     * Apply language configuration to context
     */
    fun applyLanguageConfiguration(context: Context, language: SupportedLanguage): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        var configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        if (language.isRTL) {
            configuration.setLayoutDirection(locale)
        }
        
        return context.createConfigurationContext(configuration)
    }
    
    // English strings (default)
    private fun getEnglishStrings() = LocalizedStrings(
        play = "Play",
        pause = "Pause",
        stop = "Stop",
        next = "Next",
        previous = "Previous",
        seekForward = "Seek Forward",
        seekBackward = "Seek Backward",
        fullscreen = "Fullscreen",
        exitFullscreen = "Exit Fullscreen",
        volume = "Volume",
        mute = "Mute",
        unmute = "Unmute",
        settings = "Settings",
        subtitles = "Subtitles",
        audioTracks = "Audio Tracks",
        playbackSpeed = "Playback Speed",
        quality = "Quality",
        
        browse = "Browse",
        folder = "Folder",
        file = "File",
        videos = "Videos",
        audio = "Audio",
        images = "Images",
        documents = "Documents",
        recent = "Recent",
        favorites = "Favorites",
        search = "Search",
        sortBy = "Sort By",
        name = "Name",
        size = "Size",
        date = "Date",
        type = "Type",
        
        liveStreaming = "Live Streaming",
        stream = "Stream",
        streaming = "Streaming",
        viewers = "Viewers",
        bitrate = "Bitrate",
        fps = "FPS",
        startStream = "Start Stream",
        stopStream = "Stop Stream",
        streamKey = "Stream Key",
        streamUrl = "Stream URL",
        
        cast = "Cast",
        casting = "Casting",
        castTo = "Cast to",
        connected = "Connected",
        connecting = "Connecting",
        disconnect = "Disconnect",
        noDevicesFound = "No devices found",
        castingTo = "Casting to",
        
        bookmarks = "Bookmarks",
        bookmark = "Bookmark",
        addBookmark = "Add Bookmark",
        chapters = "Chapters",
        chapter = "Chapter",
        addChapter = "Add Chapter",
        notes = "Notes",
        tags = "Tags",
        
        language = "Language",
        theme = "Theme",
        appearance = "Appearance",
        playback = "Playback",
        network = "Network",
        storage = "Storage",
        privacy = "Privacy",
        about = "About",
        version = "Version",
        developer = "Developer",
        
        ok = "OK",
        cancel = "Cancel",
        save = "Save",
        delete = "Delete",
        edit = "Edit",
        add = "Add",
        remove = "Remove",
        close = "Close",
        back = "Back",
        next_ = "Next",
        previous_ = "Previous",
        loading = "Loading",
        error = "Error",
        retry = "Retry",
        success = "Success",
        warning = "Warning",
        info = "Info",
        
        duration = "Duration",
        position = "Position",
        remaining = "Remaining",
        elapsed = "Elapsed",
        hours = "Hours",
        minutes = "Minutes",
        seconds = "Seconds",
        
        resolution = "Resolution",
        format = "Format",
        codec = "Codec",
        frameRate = "Frame Rate",
        aspectRatio = "Aspect Ratio",
        colorSpace = "Color Space",
        
        nowPlaying = "Now Playing",
        paused = "Paused",
        stopped = "Stopped",
        buffering = "Buffering",
        mediaLoaded = "Media Loaded",
        mediaError = "Media Error"
    )
    
    // Spanish strings
    private fun getSpanishStrings() = LocalizedStrings(
        play = "Reproducir",
        pause = "Pausar",
        stop = "Detener",
        next = "Siguiente",
        previous = "Anterior",
        seekForward = "Avanzar",
        seekBackward = "Retroceder",
        fullscreen = "Pantalla completa",
        exitFullscreen = "Salir de pantalla completa",
        volume = "Volumen",
        mute = "Silenciar",
        unmute = "Activar sonido",
        settings = "Configuración",
        subtitles = "Subtítulos",
        audioTracks = "Pistas de audio",
        playbackSpeed = "Velocidad de reproducción",
        quality = "Calidad",
        
        browse = "Explorar",
        folder = "Carpeta",
        file = "Archivo",
        videos = "Videos",
        audio = "Audio",
        images = "Imágenes",
        documents = "Documentos",
        recent = "Reciente",
        favorites = "Favoritos",
        search = "Buscar",
        sortBy = "Ordenar por",
        name = "Nombre",
        size = "Tamaño",
        date = "Fecha",
        type = "Tipo",
        
        liveStreaming = "Transmisión en vivo",
        stream = "Transmisión",
        streaming = "Transmitiendo",
        viewers = "Espectadores",
        bitrate = "Tasa de bits",
        fps = "FPS",
        startStream = "Iniciar transmisión",
        stopStream = "Detener transmisión",
        streamKey = "Clave de transmisión",
        streamUrl = "URL de transmisión",
        
        cast = "Transmitir",
        casting = "Transmitiendo",
        castTo = "Transmitir a",
        connected = "Conectado",
        connecting = "Conectando",
        disconnect = "Desconectar",
        noDevicesFound = "No se encontraron dispositivos",
        castingTo = "Transmitiendo a",
        
        bookmarks = "Marcadores",
        bookmark = "Marcador",
        addBookmark = "Agregar marcador",
        chapters = "Capítulos",
        chapter = "Capítulo",
        addChapter = "Agregar capítulo",
        notes = "Notas",
        tags = "Etiquetas",
        
        language = "Idioma",
        theme = "Tema",
        appearance = "Apariencia",
        playback = "Reproducción",
        network = "Red",
        storage = "Almacenamiento",
        privacy = "Privacidad",
        about = "Acerca de",
        version = "Versión",
        developer = "Desarrollador",
        
        ok = "Aceptar",
        cancel = "Cancelar",
        save = "Guardar",
        delete = "Eliminar",
        edit = "Editar",
        add = "Agregar",
        remove = "Quitar",
        close = "Cerrar",
        back = "Atrás",
        next_ = "Siguiente",
        previous_ = "Anterior",
        loading = "Cargando",
        error = "Error",
        retry = "Reintentar",
        success = "Éxito",
        warning = "Advertencia",
        info = "Información",
        
        duration = "Duración",
        position = "Posición",
        remaining = "Restante",
        elapsed = "Transcurrido",
        hours = "Horas",
        minutes = "Minutos",
        seconds = "Segundos",
        
        resolution = "Resolución",
        format = "Formato",
        codec = "Códec",
        frameRate = "Velocidad de fotogramas",
        aspectRatio = "Relación de aspecto",
        colorSpace = "Espacio de color",
        
        nowPlaying = "Reproduciendo ahora",
        paused = "Pausado",
        stopped = "Detenido",
        buffering = "Almacenando en búfer",
        mediaLoaded = "Medio cargado",
        mediaError = "Error de medio"
    )
    
    // French strings
    private fun getFrenchStrings() = LocalizedStrings(
        play = "Lire",
        pause = "Pause",
        stop = "Arrêter",
        next = "Suivant",
        previous = "Précédent",
        seekForward = "Avancer",
        seekBackward = "Reculer",
        fullscreen = "Plein écran",
        exitFullscreen = "Quitter le plein écran",
        volume = "Volume",
        mute = "Muet",
        unmute = "Activer le son",
        settings = "Paramètres",
        subtitles = "Sous-titres",
        audioTracks = "Pistes audio",
        playbackSpeed = "Vitesse de lecture",
        quality = "Qualité",
        
        browse = "Parcourir",
        folder = "Dossier",
        file = "Fichier",
        videos = "Vidéos",
        audio = "Audio",
        images = "Images",
        documents = "Documents",
        recent = "Récent",
        favorites = "Favoris",
        search = "Rechercher",
        sortBy = "Trier par",
        name = "Nom",
        size = "Taille",
        date = "Date",
        type = "Type",
        
        liveStreaming = "Diffusion en direct",
        stream = "Diffusion",
        streaming = "Diffusion en cours",
        viewers = "Spectateurs",
        bitrate = "Débit binaire",
        fps = "IPS",
        startStream = "Démarrer la diffusion",
        stopStream = "Arrêter la diffusion",
        streamKey = "Clé de diffusion",
        streamUrl = "URL de diffusion",
        
        cast = "Diffuser",
        casting = "Diffusion",
        castTo = "Diffuser vers",
        connected = "Connecté",
        connecting = "Connexion",
        disconnect = "Déconnecter",
        noDevicesFound = "Aucun appareil trouvé",
        castingTo = "Diffusion vers",
        
        bookmarks = "Signets",
        bookmark = "Signet",
        addBookmark = "Ajouter un signet",
        chapters = "Chapitres",
        chapter = "Chapitre",
        addChapter = "Ajouter un chapitre",
        notes = "Notes",
        tags = "Étiquettes",
        
        language = "Langue",
        theme = "Thème",
        appearance = "Apparence",
        playback = "Lecture",
        network = "Réseau",
        storage = "Stockage",
        privacy = "Confidentialité",
        about = "À propos",
        version = "Version",
        developer = "Développeur",
        
        ok = "OK",
        cancel = "Annuler",
        save = "Enregistrer",
        delete = "Supprimer",
        edit = "Modifier",
        add = "Ajouter",
        remove = "Supprimer",
        close = "Fermer",
        back = "Retour",
        next_ = "Suivant",
        previous_ = "Précédent",
        loading = "Chargement",
        error = "Erreur",
        retry = "Réessayer",
        success = "Succès",
        warning = "Avertissement",
        info = "Information",
        
        duration = "Durée",
        position = "Position",
        remaining = "Restant",
        elapsed = "Écoulé",
        hours = "Heures",
        minutes = "Minutes",
        seconds = "Secondes",
        
        resolution = "Résolution",
        format = "Format",
        codec = "Codec",
        frameRate = "Fréquence d'images",
        aspectRatio = "Rapport d'aspect",
        colorSpace = "Espace colorimétrique",
        
        nowPlaying = "En cours de lecture",
        paused = "En pause",
        stopped = "Arrêté",
        buffering = "Mise en mémoire tampon",
        mediaLoaded = "Média chargé",
        mediaError = "Erreur de média"
    )
    
    // German strings
    private fun getGermanStrings() = LocalizedStrings(
        play = "Wiedergeben",
        pause = "Pausieren",
        stop = "Stoppen",
        next = "Weiter",
        previous = "Zurück",
        seekForward = "Vorspulen",
        seekBackward = "Zurückspulen",
        fullscreen = "Vollbild",
        exitFullscreen = "Vollbild verlassen",
        volume = "Lautstärke",
        mute = "Stumm",
        unmute = "Ton einschalten",
        settings = "Einstellungen",
        subtitles = "Untertitel",
        audioTracks = "Audiospuren",
        playbackSpeed = "Wiedergabegeschwindigkeit",
        quality = "Qualität",
        
        browse = "Durchsuchen",
        folder = "Ordner",
        file = "Datei",
        videos = "Videos",
        audio = "Audio",
        images = "Bilder",
        documents = "Dokumente",
        recent = "Zuletzt verwendet",
        favorites = "Favoriten",
        search = "Suchen",
        sortBy = "Sortieren nach",
        name = "Name",
        size = "Größe",
        date = "Datum",
        type = "Typ",
        
        liveStreaming = "Live-Streaming",
        stream = "Stream",
        streaming = "Streaming",
        viewers = "Zuschauer",
        bitrate = "Bitrate",
        fps = "FPS",
        startStream = "Stream starten",
        stopStream = "Stream stoppen",
        streamKey = "Stream-Schlüssel",
        streamUrl = "Stream-URL",
        
        cast = "Übertragen",
        casting = "Übertragung",
        castTo = "Übertragen an",
        connected = "Verbunden",
        connecting = "Verbindung wird hergestellt",
        disconnect = "Trennen",
        noDevicesFound = "Keine Geräte gefunden",
        castingTo = "Übertragung an",
        
        bookmarks = "Lesezeichen",
        bookmark = "Lesezeichen",
        addBookmark = "Lesezeichen hinzufügen",
        chapters = "Kapitel",
        chapter = "Kapitel",
        addChapter = "Kapitel hinzufügen",
        notes = "Notizen",
        tags = "Tags",
        
        language = "Sprache",
        theme = "Design",
        appearance = "Erscheinungsbild",
        playback = "Wiedergabe",
        network = "Netzwerk",
        storage = "Speicher",
        privacy = "Datenschutz",
        about = "Über",
        version = "Version",
        developer = "Entwickler",
        
        ok = "OK",
        cancel = "Abbrechen",
        save = "Speichern",
        delete = "Löschen",
        edit = "Bearbeiten",
        add = "Hinzufügen",
        remove = "Entfernen",
        close = "Schließen",
        back = "Zurück",
        next_ = "Weiter",
        previous_ = "Zurück",
        loading = "Laden",
        error = "Fehler",
        retry = "Wiederholen",
        success = "Erfolg",
        warning = "Warnung",
        info = "Information",
        
        duration = "Dauer",
        position = "Position",
        remaining = "Verbleibend",
        elapsed = "Vergangen",
        hours = "Stunden",
        minutes = "Minuten",
        seconds = "Sekunden",
        
        resolution = "Auflösung",
        format = "Format",
        codec = "Codec",
        frameRate = "Bildrate",
        aspectRatio = "Seitenverhältnis",
        colorSpace = "Farbraum",
        
        nowPlaying = "Wird wiedergegeben",
        paused = "Pausiert",
        stopped = "Gestoppt",
        buffering = "Puffern",
        mediaLoaded = "Medium geladen",
        mediaError = "Medienfehler"
    )
    
    // Placeholder implementations for other languages
    // Italian strings
    private fun getItalianStrings() = LocalizedStrings(
        play = "Riproduci",
        pause = "Pausa",
        stop = "Ferma",
        next = "Successivo",
        previous = "Precedente",
        seekForward = "Avanti veloce",
        seekBackward = "Indietro veloce",
        fullscreen = "Schermo intero",
        exitFullscreen = "Esci da schermo intero",
        volume = "Volume",
        mute = "Silenzia",
        unmute = "Attiva audio",
        settings = "Impostazioni",
        subtitles = "Sottotitoli",
        audioTracks = "Tracce audio",
        playbackSpeed = "Velocità di riproduzione",
        quality = "Qualità",
        
        browse = "Sfoglia",
        folder = "Cartella",
        file = "File",
        videos = "Video",
        audio = "Audio",
        images = "Immagini",
        documents = "Documenti",
        recent = "Recenti",
        favorites = "Preferiti",
        search = "Cerca",
        sortBy = "Ordina per",
        name = "Nome",
        size = "Dimensione",
        date = "Data",
        type = "Tipo",
        
        liveStreaming = "Streaming dal vivo",
        stream = "Stream",
        streaming = "Streaming",
        viewers = "Spettatori",
        bitrate = "Bitrate",
        fps = "FPS",
        startStream = "Avvia streaming",
        stopStream = "Ferma streaming",
        streamKey = "Chiave stream",
        streamUrl = "URL stream",
        
        cast = "Trasmetti",
        casting = "Trasmissione",
        castTo = "Trasmetti a",
        connected = "Connesso",
        connecting = "Connessione",
        disconnect = "Disconnetti",
        noDevicesFound = "Nessun dispositivo trovato",
        castingTo = "Trasmissione a",
        
        bookmarks = "Segnalibri",
        bookmark = "Segnalibro",
        addBookmark = "Aggiungi segnalibro",
        chapters = "Capitoli",
        chapter = "Capitolo",
        addChapter = "Aggiungi capitolo",
        notes = "Note",
        tags = "Tag",
        
        language = "Lingua",
        theme = "Tema",
        appearance = "Aspetto",
        playback = "Riproduzione",
        network = "Rete",
        storage = "Archiviazione",
        privacy = "Privacy",
        about = "Informazioni",
        version = "Versione",
        developer = "Sviluppatore",
        
        ok = "OK",
        cancel = "Annulla",
        save = "Salva",
        delete = "Elimina",
        edit = "Modifica",
        add = "Aggiungi",
        remove = "Rimuovi",
        close = "Chiudi",
        back = "Indietro",
        next_ = "Avanti",
        previous_ = "Indietro",
        loading = "Caricamento",
        error = "Errore",
        retry = "Riprova",
        success = "Successo",
        warning = "Avviso",
        info = "Info",
        
        duration = "Durata",
        position = "Posizione",
        remaining = "Rimanente",
        elapsed = "Trascorso",
        hours = "Ore",
        minutes = "Minuti",
        seconds = "Secondi",
        
        resolution = "Risoluzione",
        format = "Formato",
        codec = "Codec",
        frameRate = "Frame rate",
        aspectRatio = "Proporzioni",
        colorSpace = "Spazio colore",
        
        nowPlaying = "In riproduzione",
        paused = "In pausa",
        stopped = "Fermato",
        buffering = "Buffering",
        mediaLoaded = "Media caricato",
        mediaError = "Errore media"
    )
    
    // Portuguese strings
    private fun getPortugueseStrings() = LocalizedStrings(
        play = "Reproduzir",
        pause = "Pausar",
        stop = "Parar",
        next = "Próximo",
        previous = "Anterior",
        seekForward = "Avançar",
        seekBackward = "Retroceder",
        fullscreen = "Tela cheia",
        exitFullscreen = "Sair da tela cheia",
        volume = "Volume",
        mute = "Silenciar",
        unmute = "Ativar som",
        settings = "Configurações",
        subtitles = "Legendas",
        audioTracks = "Faixas de áudio",
        playbackSpeed = "Velocidade de reprodução",
        quality = "Qualidade",
        
        browse = "Navegar",
        folder = "Pasta",
        file = "Arquivo",
        videos = "Vídeos",
        audio = "Áudio",
        images = "Imagens",
        documents = "Documentos",
        recent = "Recentes",
        favorites = "Favoritos",
        search = "Pesquisar",
        sortBy = "Ordenar por",
        name = "Nome",
        size = "Tamanho",
        date = "Data",
        type = "Tipo",
        
        liveStreaming = "Transmissão ao vivo",
        stream = "Transmissão",
        streaming = "Transmitindo",
        viewers = "Espectadores",
        bitrate = "Taxa de bits",
        fps = "FPS",
        startStream = "Iniciar transmissão",
        stopStream = "Parar transmissão",
        streamKey = "Chave de transmissão",
        streamUrl = "URL de transmissão",
        
        cast = "Transmitir",
        casting = "Transmitindo",
        castTo = "Transmitir para",
        connected = "Conectado",
        connecting = "Conectando",
        disconnect = "Desconectar",
        noDevicesFound = "Nenhum dispositivo encontrado",
        castingTo = "Transmitindo para",
        
        bookmarks = "Marcadores",
        bookmark = "Marcador",
        addBookmark = "Adicionar marcador",
        chapters = "Capítulos",
        chapter = "Capítulo",
        addChapter = "Adicionar capítulo",
        notes = "Notas",
        tags = "Tags",
        
        language = "Idioma",
        theme = "Tema",
        appearance = "Aparência",
        playback = "Reprodução",
        network = "Rede",
        storage = "Armazenamento",
        privacy = "Privacidade",
        about = "Sobre",
        version = "Versão",
        developer = "Desenvolvedor",
        
        ok = "OK",
        cancel = "Cancelar",
        save = "Salvar",
        delete = "Excluir",
        edit = "Editar",
        add = "Adicionar",
        remove = "Remover",
        close = "Fechar",
        back = "Voltar",
        next_ = "Próximo",
        previous_ = "Anterior",
        loading = "Carregando",
        error = "Erro",
        retry = "Tentar novamente",
        success = "Sucesso",
        warning = "Aviso",
        info = "Informação",
        
        duration = "Duração",
        position = "Posição",
        remaining = "Restante",
        elapsed = "Decorrido",
        hours = "Horas",
        minutes = "Minutos",
        seconds = "Segundos",
        
        resolution = "Resolução",
        format = "Formato",
        codec = "Codec",
        frameRate = "Taxa de quadros",
        aspectRatio = "Proporção",
        colorSpace = "Espaço de cor",
        
        nowPlaying = "Reproduzindo agora",
        paused = "Pausado",
        stopped = "Parado",
        buffering = "Carregando",
        mediaLoaded = "Mídia carregada",
        mediaError = "Erro de mídia"
    )
    
    // Russian strings
    private fun getRussianStrings() = LocalizedStrings(
        play = "Воспроизвести",
        pause = "Пауза",
        stop = "Стоп",
        next = "Следующий",
        previous = "Предыдущий",
        seekForward = "Перемотать вперед",
        seekBackward = "Перемотать назад",
        fullscreen = "Полный экран",
        exitFullscreen = "Выйти из полноэкранного режима",
        volume = "Громкость",
        mute = "Выключить звук",
        unmute = "Включить звук",
        settings = "Настройки",
        subtitles = "Субтитры",
        audioTracks = "Аудиодорожки",
        playbackSpeed = "Скорость воспроизведения",
        quality = "Качество",
        
        browse = "Обзор",
        folder = "Папка",
        file = "Файл",
        videos = "Видео",
        audio = "Аудио",
        images = "Изображения",
        documents = "Документы",
        recent = "Недавние",
        favorites = "Избранное",
        search = "Поиск",
        sortBy = "Сортировать по",
        name = "Имя",
        size = "Размер",
        date = "Дата",
        type = "Тип",
        
        liveStreaming = "Прямая трансляция",
        stream = "Поток",
        streaming = "Трансляция",
        viewers = "Зрители",
        bitrate = "Битрейт",
        fps = "Кадров в секунду",
        startStream = "Начать трансляцию",
        stopStream = "Остановить трансляцию",
        streamKey = "Ключ потока",
        streamUrl = "URL потока",
        
        cast = "Транслировать",
        casting = "Трансляция",
        castTo = "Транслировать на",
        connected = "Подключено",
        connecting = "Подключение",
        disconnect = "Отключить",
        noDevicesFound = "Устройства не найдены",
        castingTo = "Трансляция на",
        
        bookmarks = "Закладки",
        bookmark = "Закладка",
        addBookmark = "Добавить закладку",
        chapters = "Главы",
        chapter = "Глава",
        addChapter = "Добавить главу",
        notes = "Заметки",
        tags = "Теги",
        
        language = "Язык",
        theme = "Тема",
        appearance = "Внешний вид",
        playback = "Воспроизведение",
        network = "Сеть",
        storage = "Хранилище",
        privacy = "Конфиденциальность",
        about = "О программе",
        version = "Версия",
        developer = "Разработчик",
        
        ok = "ОК",
        cancel = "Отмена",
        save = "Сохранить",
        delete = "Удалить",
        edit = "Редактировать",
        add = "Добавить",
        remove = "Удалить",
        close = "Закрыть",
        back = "Назад",
        next_ = "Далее",
        previous_ = "Назад",
        loading = "Загрузка",
        error = "Ошибка",
        retry = "Повторить",
        success = "Успешно",
        warning = "Предупреждение",
        info = "Информация",
        
        duration = "Длительность",
        position = "Позиция",
        remaining = "Осталось",
        elapsed = "Прошло",
        hours = "Часы",
        minutes = "Минуты",
        seconds = "Секунды",
        
        resolution = "Разрешение",
        format = "Формат",
        codec = "Кодек",
        frameRate = "Частота кадров",
        aspectRatio = "Соотношение сторон",
        colorSpace = "Цветовое пространство",
        
        nowPlaying = "Сейчас играет",
        paused = "Приостановлено",
        stopped = "Остановлено",
        buffering = "Буферизация",
        mediaLoaded = "Медиа загружено",
        mediaError = "Ошибка медиа"
    )
    private fun getChineseStrings() = AdditionalTranslations.getChineseStrings()
    private fun getJapaneseStrings() = AdditionalTranslations.getJapaneseStrings()
    private fun getKoreanStrings() = AdditionalTranslations.getKoreanStrings()
    private fun getArabicStrings() = AdditionalTranslations.getArabicStrings()
    private fun getHindiStrings() = AdditionalTranslations.getHindiStrings()
    private fun getTurkishStrings() = AdditionalTranslations.getTurkishStrings()
    private fun getPolishStrings() = AdditionalTranslations.getPolishStrings()
    private fun getDutchStrings() = AdditionalTranslations.getDutchStrings()
}

/**
 * ViewModel for localization management
 */
class LocalizationViewModel(private val localizationManager: LocalizationManager) : ViewModel() {
    
    val currentLanguage = localizationManager.currentLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SupportedLanguage("en", "English", "English", "🇺🇸")
    )
    
    val useSystemLanguage = localizationManager.useSystemLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    
    private val _localizedStrings = MutableStateFlow<LocalizedStrings?>(null)
    val localizedStrings: StateFlow<LocalizedStrings?> = _localizedStrings.asStateFlow()
    
    init {
        // Load localized strings when language changes
        viewModelScope.launch {
            currentLanguage.collect { language ->
                _localizedStrings.value = localizationManager.getLocalizedStrings(language)
            }
        }
    }
    
    fun getSupportedLanguages(): List<SupportedLanguage> {
        return localizationManager.getSupportedLanguages()
    }
    
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            localizationManager.setLanguage(languageCode)
        }
    }
    
    fun setUseSystemLanguage(useSystem: Boolean) {
        viewModelScope.launch {
            localizationManager.setUseSystemLanguage(useSystem)
        }
    }
    
    fun applyLanguageConfiguration(context: Context, language: SupportedLanguage): Context {
        return localizationManager.applyLanguageConfiguration(context, language)
    }
}

/**
 * Composable for language selection screen
 */
@Composable
fun LanguageSelectionScreen(
    currentLanguage: SupportedLanguage,
    useSystemLanguage: Boolean,
    supportedLanguages: List<SupportedLanguage>,
    onLanguageSelected: (String) -> Unit,
    onUseSystemLanguageChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Language / Idioma / Langue",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // System language toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use System Language",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Follow device language settings",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                
                Switch(
                    checked = useSystemLanguage,
                    onCheckedChange = onUseSystemLanguageChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00BCD4),
                        checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f)
                    )
                )
            }
        }
        
        // Language list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(supportedLanguages) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language.code == currentLanguage.code,
                    isEnabled = !useSystemLanguage,
                    onClick = { onLanguageSelected(language.code) }
                )
            }
        }
    }
}

/**
 * Language selection item
 */
@Composable
private fun LanguageItem(
    language: SupportedLanguage,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF00BCD4).copy(alpha = 0.2f)
            else Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag
            Text(
                text = language.flag,
                fontSize = 24.sp,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Language info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = language.name,
                        color = if (isEnabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                    
                    if (language.completionPercentage < 100) {
                        Text(
                            text = "${language.completionPercentage}%",
                            color = Color(0xFFFFC107),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (language.isRTL) {
                        Text(
                            text = "RTL",
                            color = Color(0xFF00BCD4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Composable provider for localized strings
 */
@Composable
fun LocalizationProvider(
    localizationViewModel: LocalizationViewModel,
    content: @Composable (LocalizedStrings?) -> Unit
) {
    val localizedStrings by localizationViewModel.localizedStrings.collectAsState()
    content(localizedStrings)
}