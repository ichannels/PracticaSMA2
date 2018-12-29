import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;


public class UsersServletReader {
	public Users getUsers() {
		return users;
	}

	public void setUsers(Users users) {
		this.users = users;
	}

	private static Users users;

	public static void main(String[] args) {
		try (InputStream xml = UsersServletReader.class
				.getResourceAsStream("users.xml");) {
			JAXBContext jaxbContext = JAXBContext.newInstance(Users.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			users = (Users) jaxbUnmarshaller.unmarshal(xml);
			System.out.println(users.toString());
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

	}
}