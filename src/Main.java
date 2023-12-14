import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class Main {
    private static final Scanner scan=new Scanner(System.in);
    private static final String url = "jdbc:mysql://localhost:3306/social_network" +
            "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

    private static final String usuario="root";
    private static final String password="root";
    private static Connection con;
    private static int userID=-1;


    public static void main(String[] args) {
        try {
            con = DriverManager.getConnection(url, usuario, password);

        } catch (SQLException e) {
            e.printStackTrace();
        }


        int n = 0;
        do {

                System.out.println("Introduce opción:\n" +
                        "\t1. Registrarse\n" +
                        "\t2. Login");
                n = scan.nextInt();

                switch (n) {
                    case 1:
                        String[] reg = form("REGISTRO");
                        register(con, reg[0], reg[1], reg[2]);
                        break;
                    case 2:
                        String[] log = form2("LOGIN");
                        login(con, log[0], log[1]);
                        if (userID != -1) {
                            menuTwitter(con, scan);
                        }
                        break;
                    default:
                        System.out.println("Saliendo...");
                        break;
                }

        } while (n == 1 || n == 2);

        try {
            con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    private static void register(Connection conexion, String user, String email, String password) {
        String passRegHash = BCrypt.hashpw(password, BCrypt.gensalt());
        PreparedStatement sentencia = null;
        try {

            sentencia = conexion.prepareStatement("INSERT INTO users (username, email, password, " +
                    "description, createDate) VALUES (?, ?, ?, ?, NOW())");

            sentencia.setString(1, user);
            sentencia.setString(2, email);
            sentencia.setString(3, passRegHash);
            sentencia.setString(4, "Descripción de usuario");
            sentencia.executeUpdate();
            System.out.println("Registrado con éxito!");
            sentencia.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

   private static String[] form(String str) {

        System.out.println("------------------------------------ " + str + " ------------------------------------");
        System.out.println("Introduce nombre:");
        String userReg = scan.next();
        System.out.println("Introduce email:");
        String emailReg = scan.next();
        System.out.println("Introduce contraseña:");
        String passReg = scan.next();
        String[] res = {userReg,emailReg,passReg};
        return res;
    }
   private static String[] form2(String str) {

        System.out.println("------------------------------------ " + str + " ------------------------------------");
        System.out.println("Introduce nombre:");
        String userReg = scan.next();
        System.out.println("Introduce contraseña:");
        String passReg = scan.next();
        String[] res = {userReg,passReg};
        return res;
    }
   private static void menu(String str){
        System.out.println("------------------------------------ " + str + " ------------------------------------");
        System.out.println("1. Mostrar Tweets de seguidores");
        System.out.println("2. Mostrar Tweets");
        System.out.println("3. Tweetear");
        System.out.println("4. Mostrar tu perfil");
        System.out.println("5. Mostrar todos los perfiles");
        System.out.println("6. Mostrar tus tweets");
        System.out.println("7. Borrar tweets");
        System.out.println("8. Mostrar tus seguidos");
        System.out.println("9. Mostrar tus seguidores");
        System.out.println("10. Mostrar otros perfiles");
        System.out.println("11. Seguir");
        System.out.println("12. Dejar de seguir");
        System.out.println("-------------------------------------------------------------------------------------");
    }
    private static void menuTwitter(Connection con, Scanner scan){
        int n;
        do {
            menu("MENU");
            switch (n = scan.nextInt()) {
                case 1:
                    showFollowedTweets(con);
                    break;
                case 2:
                    showTweets(con);
                    break;
                case 3:
                    System.out.println("¿Qué vas a tweetear?");
                    scan.nextLine(); // Consumir la nueva línea pendiente
                    String texto = scan.nextLine();
                    tweetear(con, texto);
                    break;
                case 4:
                    showYourProfile(con);
                    break;
                case 5:
                    showAllProfiles(con);
                    break;
                case 6:
                    showYourTweets(con);
                    break;
                case 7:
                    System.out.println("Introduce tu id: ");
                    int ide=scan.nextInt();
                    deleteTweets(con,ide);
                    break;
                case 8:
                    showYourFollows(con);
                    break;
                case 9:
                    showYourFollowers(con);
                    break;
                case 10:
                    System.out.println("Que perfil buscas?: ");
                    String nombre=scan.next();
                    showOtherProfile(con,nombre);
                    break;
                case 11:
                    System.out.println("Introduce el id:");
                    int ide2=scan.nextInt();
                    follow(con,ide2);
                    break;
                case 12:
                    System.out.println("Introduce el id:");
                    int ide3=scan.nextInt();
                    unFollow(con,ide3);
                default:
                    System.out.println("Saliendo...");
                    break;
            }
        } while (n <= 12 && n>0);
    }

    public static void login(Connection conexion, String user, String password) {
        PreparedStatement sentencia = null;
        try {
            sentencia = conexion.prepareStatement("SELECT id, password FROM users WHERE username = ?");
            sentencia.setString(1, user);

            ResultSet result = sentencia.executeQuery();
            boolean esCorrecta = false;

            if (result.next()) {
                int userId = result.getInt("id");
                String storedPassword = result.getString("password");
                esCorrecta = BCrypt.checkpw(password, storedPassword);
                if (esCorrecta) {
                    userID = userId; // Actualizar userID con el ID del usuario autenticado
                }
            }

            System.out.println(esCorrecta ? "Logueado con éxito" : "Contraseña incorrecta");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (sentencia != null) {
                try {
                    sentencia.close();
                } catch (SQLException e) {

                }
            }
        }
    }
    public static void showFollowedTweets(Connection conexion) {
        if (userID == -1) {
            System.out.println("Inicie sesión antes de ver los tweets.");
            return;
        }

        PreparedStatement sentencia = null;
        try {

            sentencia = conexion.prepareStatement("SELECT u.username, p.text, p.createDate\n" +
                    "FROM users u\n" +
                    "JOIN publications p ON u.id = p.userId\n" +
                    "JOIN follows f ON u.id = f.users_id\n" +
                    "WHERE f.userToFollowId = ?\n" +
                    "ORDER BY p.createDate DESC");

            sentencia.setInt(1, userID);
            ResultSet resultSet = sentencia.executeQuery();
            boolean hayTweets=false;

            while (resultSet.next()) {
                hayTweets=true;
                String username = resultSet.getString("username");
                String tweetText = resultSet.getString("text");
                String tweetDate = resultSet.getString("createDate"); // Corrección aquí

                System.out.println("Usuario: " + username);
                System.out.println("Tweet: " + tweetText);
                System.out.println("Fecha de publicación: " + tweetDate);
                System.out.println();
            }
            if(!hayTweets){
                System.out.println("No hay tweets de seguidores.");
            }
            sentencia.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static void showTweets(Connection conexion){
        PreparedStatement sentencia = null;
        try {

            sentencia = conexion.prepareStatement("SELECT \n" +
                    "    u.username, p.text, p.createDate\n" +
                    "FROM\n" +
                    "    users u\n" +
                    "        JOIN\n" +
                    "    publications p ON u.id = p.userId\n" +
                    "ORDER BY p.createDate DESC");


            ResultSet resultSet = sentencia.executeQuery();

            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String tweetText = resultSet.getString("text");
                String tweetDate = resultSet.getString("createDate");

                System.out.println("Usuario: " + username);
                System.out.println("Tweet: " + tweetText);
                System.out.println("Fecha de publicación: " + tweetDate);
                System.out.println();
            }
            sentencia.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static void tweetear(Connection conexion,String texto){


        PreparedStatement sentencia = null;
        try {
            // Obtener la fecha actual
            long currentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String currentDate = sdf.format(new Date(currentTimeMillis));

            String sql = "INSERT INTO publications (userId, text, createDate) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = conexion.prepareStatement(sql  );
            preparedStatement.setInt(1, userID);
            preparedStatement.setString(2, texto);
            preparedStatement.setString(3, currentDate);
            preparedStatement.executeUpdate();
            System.out.println("Tweet publicado con éxito.");
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error al twittear: " + e.getMessage());
        }
    }
    private static void showYourProfile(Connection conexion) {
        if (userID == -1) {
            System.out.println("Inicie sesión para ver su perfil.");
            return;
        }

        try {
            PreparedStatement statement = conexion.prepareStatement("SELECT username, email, " +
                    "description, createDate " +
                    "FROM users WHERE id = ?");
            statement.setInt(1, userID);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String username = resultSet.getString("username");
                String email = resultSet.getString("email");
                String description = resultSet.getString("description");
                String createDate = resultSet.getString("createDate");

                System.out.println("Nombre de Usuario: " + username);
                System.out.println("Email: " + email);
                System.out.println("Descripción: " + description);
                System.out.println("Fecha de Registro: " + createDate);
            } else {
                System.out.println("No se encontraron datos de perfil para el usuario.");
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener el perfil: " + e.getMessage());
        }
    }
    private static void showAllProfiles(Connection conexion){
        String sql = "SELECT id, username, email, description, createDate FROM users WHERE id != ?";
        try  {
            PreparedStatement preparedStatement = conexion.prepareStatement(sql);
            preparedStatement.setInt(1, userID);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String email = resultSet.getString("email");
                String description = resultSet.getString("description");
                String createDate = resultSet.getString("createDate");

                System.out.println("ID: " + id + ", Username: " + username + ", Email: " + email
                        + ", Description: " + description + ", Create Date: " + createDate);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void showYourTweets(Connection conexion){
        String sql="SELECT \n" +
                "    p.id, u.username, p.text, p.createDate\n" +
                "FROM\n" +
                "    publications p\n" +
                "        JOIN\n" +
                "    users u ON (u.id = p.userID) where u.id=?";
        try {
            PreparedStatement statement=conexion.prepareStatement(sql);
            statement.setInt(1, userID);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                int id = result.getInt("id");
                String username = result.getString("username");
                String text = result.getString("text");
                String createDate = result.getString("createDate");

                System.out.println("ID: " + id + ", Username: " + username + ", Email: "
                        + ", Text: " + text + ", Create Date: " + createDate);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static void deleteTweets(Connection conexion, int id) {
        String sql = "DELETE FROM publications WHERE id = ? AND userId = ?";
        try {
            PreparedStatement statement = conexion.prepareStatement(sql);
            statement.setInt(1, id);
            statement.setInt(2, userID);
            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas == 0) {
                System.out.println("No se encontraron registros para eliminar.");
            } else {
                System.out.println("Se eliminó la publicación exitosamente.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void showYourFollows(Connection conexion){
        String sql = "SELECT u.username, u.description " +
                "FROM follows f " +
                "JOIN users u ON f.userToFollowId = u.id " +
                "WHERE f.users_id = ?";
        try {
            PreparedStatement statement=conexion.prepareStatement(sql);
            statement.setInt(1,userID);
            ResultSet result = statement.executeQuery();

            System.out.println("Usuarios que sigues:");
            while(result.next()){
                String username=result.getString("username");
                String description=result.getString("description");
                System.out.println( " Username: " + username +
                        ", Description: " + description);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static void showYourFollowers(Connection conexion) {
        String sql = "SELECT u.username, u.email " +
                "FROM follows f " +
                "JOIN users u ON (f.users_id=u.id) " +
                "WHERE f.userToFollowId = ?";

        try {
            PreparedStatement statement = conexion.prepareStatement(sql);
            statement.setInt(1, userID);
            ResultSet result = statement.executeQuery();

            System.out.println("Usuarios que te siguen:");
            while (result.next()) {
                String username = result.getString("username");
                String email = result.getString("email");
                System.out.println("Username: " + username);
                System.out.println("Email: " + email);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener tus seguidores", e);
        }
    }
    private static void showOtherProfile(Connection conexion,String nombre){
        String sql="select * from users where username=?";
        try {
            PreparedStatement statement = conexion.prepareStatement(sql);
            statement.setString(1, nombre);
            ResultSet result = statement.executeQuery();

            while (result.next()) {
                int id = result.getInt("id");
                String user = result.getString("username");
                String email = result.getString("email");
                System.out.println("ID: " + id);
                System.out.println("Username: " + user);
                System.out.println("Email: " + email);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener el perfil del usuario", e);
        }
    }
    private static void follow(Connection conexion,int id){
        String sql = "INSERT INTO follows (users_id, userToFollowId) VALUES (?, ?)";
        try {
            PreparedStatement preparedStatement=conexion.prepareStatement(sql);
            preparedStatement.setInt(1,userID);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
            System.out.println("Usuario " + userID + " sigue a " + id + " con éxito.");
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener el perfil del usuario", e);
        }

    }
    private static void unFollow(Connection conexion,int id){
        String sql = "DELETE FROM follows WHERE users_id = ? AND userToFollowId = ?";
        try {
            PreparedStatement preparedStatement=conexion.prepareStatement(sql);
            preparedStatement.setInt(1,userID);
            preparedStatement.setInt(2, id);
            int filaAfectada= preparedStatement.executeUpdate();
            if(filaAfectada >0){
                System.out.println("Usuario " + userID + " ya no sigue a " + id + " con éxito.");
            }else {
                System.out.println("Usuario " + userID + " no seguía a " + id);
            }
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener el perfil del usuario", e);
        }
    }

}