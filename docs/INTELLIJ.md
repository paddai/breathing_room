# IntelliJ settings that stop the red "package does not exist" errors

The terminal command `.\mvnw.cmd clean compile` is the truth. IntelliJ keeps its **own** list of libraries, and that list goes stale after switching Git branches, editing `pom.xml`, or a partial download. These settings make it stay in sync.

## One-time setup
1. **File > Project Structure > Project**: set the SDK to a **Java 21** JDK and the language level to 21.
2. **Settings > Build, Execution, Deployment > Build Tools > Maven**:
   - Maven home path: **Use Maven wrapper**.
   - JDK for importer: **Project JDK**.
   - If your version shows "Reload project after changes in the build scripts", set it to **Any changes**.
3. **Settings > Build, Execution, Deployment > Build Tools > Maven > Runner**: tick **Delegate IDE build/run actions to Maven**. IntelliJ then builds with Maven, the same as the terminal, so a stale IDE library list can no longer break Build or Run.
4. **Settings > Build, Execution, Deployment > Compiler > Annotation Processors**: tick **Enable annotation processing** (needed if you use Lombok).
5. Open the **Maven** tool window. `backend` should be listed. If it is not, right-click `backend\pom.xml` and choose **Add as Maven Project**.

## After switching branches or changing pom.xml
1. **File > Reload All from Disk**.
2. Maven window: click **Reload All Maven Projects** (the circular arrows) and wait for the progress bar.
3. If the red marks stay: close IntelliJ, delete `backend\target` and the `.idea` folder, reopen the **repo root** folder, and let it import the Maven project again.

## Do not commit
`.idea`, `target`, `node_modules` and `.env` belong in `.gitignore` so a different computer's settings never break yours.
