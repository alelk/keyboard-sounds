import java.io.{ByteArrayInputStream, IOException}
import java.nio.file.{Files, Paths}
import java.util.logging.{Level, Logger}

import com.typesafe.config.{Config, ConfigFactory}
import javax.sound.sampled.{AudioSystem, Clip}
import org.jnativehook.GlobalScreen
import org.jnativehook.keyboard.{NativeKeyEvent, NativeKeyListener}
import org.jnativehook.mouse.{NativeMouseEvent, NativeMouseListener}

import scala.io.StdIn
import scala.jdk.CollectionConverters._
import scala.util.{Random, Try}

class SoundsCfg(cfg: Config, baseDirectory: String) {
  private val keyNames = cfg.entrySet().asScala.map(_.getKey)
  private val keySoundFiles = keyNames.toList.map(n => (n.toLowerCase, cfg.getStringList(n).asScala.toList)).toMap
  private val allFileNames = keySoundFiles.values.flatten.toList.distinct
  private val sounds = allFileNames.map { name =>
    val path = Paths.get(baseDirectory, name)
    try {
      val bytes = Files.readAllBytes(path)
      (name, bytes)
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Unable read file: ${path.toAbsolutePath}", e)
    }
  }.toMap

  def soundByKeyText(k: String): Option[Array[Byte]] = for {
    files <- keySoundFiles.get(k.toLowerCase) orElse keySoundFiles.get("_") orElse keySoundFiles.get("default")
    fName <- files.lift(Random.nextInt(files.length))
    sound <- sounds.get(fName)
  } yield sound
}

case class ClipPool(clips: List[Clip]) {
  def play(bytes: Array[Byte]): Unit =
    clips.filterNot(_.isActive).headOption foreach { clip =>
      if (clip.isOpen) clip.close()
      clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes)))
      clip.setFramePosition(0)
      clip.start()
    }
}

object KeyboardSoundApp extends App {
  {
    val logger = Logger.getLogger(classOf[GlobalScreen].getPackage.getName)
    logger.setLevel(Level.WARNING)
    logger.setUseParentHandlers(false)
  }

  private val profilesCfg = ConfigFactory.load().getConfig("profiles")
  private val profiles = profilesCfg.entrySet().asScala.map(_.getKey.takeWhile(_ != '.')).toList
    .sorted
    .zipWithIndex.map { case (a, b) => b -> a }
    .toMap

  println("Select sounds profile:")
  profiles.foreach { case (idx, name) => println(s"  $idx - $name") }
  (0 to 10).to(LazyList) flatMap (_ => Try(StdIn.readInt).toOption) flatMap profiles.get take 1 headOption match {
    case None => println("Incorrect input.")
    case Some(profile) =>
      println(s"Profile you choose: $profile")
      val profileCfg = profilesCfg.getConfig(profile)
      val baseDirectory = profileCfg.getString("base-directory")
      val keyboardSoundsCfg = new SoundsCfg(profileCfg.getConfig("key-sounds"), baseDirectory)
      val mouseSoundsCfg = new SoundsCfg(profileCfg.getConfig("mouse-sounds"), baseDirectory)
      val clipPool = ClipPool((1 to 10).toList.map(_ => AudioSystem.getClip()))

      val keyListener = new NativeKeyListener {
        override def nativeKeyTyped(e: NativeKeyEvent): Unit = ()

        override def nativeKeyPressed(e: NativeKeyEvent): Unit = {
          val keyText = NativeKeyEvent.getKeyText(e.getKeyCode)
          keyboardSoundsCfg.soundByKeyText(keyText) foreach clipPool.play
        }

        override def nativeKeyReleased(e: NativeKeyEvent): Unit = ()
      }

      val mouseListener = new NativeMouseListener {
        override def nativeMouseClicked(nativeMouseEvent: NativeMouseEvent): Unit = ()

        override def nativeMousePressed(nativeMouseEvent: NativeMouseEvent): Unit = {
          val btnName = nativeMouseEvent.getButton match {
            case 1 => "left"
            case 2 => "wheel"
            case 3 => "right"
            case _ => ""
          }
          mouseSoundsCfg.soundByKeyText(btnName) foreach clipPool.play
        }

        override def nativeMouseReleased(nativeMouseEvent: NativeMouseEvent): Unit = ()
      }

      GlobalScreen.registerNativeHook()
      GlobalScreen.addNativeKeyListener(keyListener)
      GlobalScreen.addNativeMouseListener(mouseListener)
      StdIn.readLine("Press any key to exit...")
      System.exit(0)
  }
}