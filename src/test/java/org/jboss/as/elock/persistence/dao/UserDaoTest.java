package org.jboss.as.elock.persistence.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.elock.model.Card;
import org.jboss.as.elock.model.Door;
import org.jboss.as.elock.model.User;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserDaoTest extends TestCase {
	
	@Deployment
	public static Archive<?> createDeployment() {
		return ShrinkWrap.create(WebArchive.class, "test.war")
				.addPackage(UserDao.class.getPackage())
				.addClasses(User.class, Card.class, Door.class)
				.addAsResource("META-INF/persistence.xml")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@PersistenceContext
	EntityManager em;
	
	@Inject
	UserDao userDao;
	
	@Inject
	UserTransaction utx;

	@Before
	public void prepareTest() throws Exception {
		userDao.setEntityManager(em);
		clearData();
		startTransaction();
	}
	
	@After
	public void commitTransaction() throws Exception {
		utx.commit();
	}
	
	private void clearData() throws Exception {
		utx.begin();
		em.joinTransaction();
		em.createQuery("delete from User").executeUpdate();
		utx.commit();
	}
	
	private void startTransaction() throws Exception {
		utx.begin();
		em.joinTransaction();
	}

	private User setUpUserObject() {
		User testUser = new User();
		testUser.setName("Jim Bob");
		testUser.setBirthdate(new Date());
		return testUser;
	}
	
	// actual tests -----------------------------------------------------------

	@Test
	public void testCreate() throws Exception {
		User user = setUpUserObject();

		userDao.create(user);

		User expected = (User) em.createQuery("FROM User u WHERE u.id = :id").setParameter("id", user.getId()).getSingleResult();
		assertNotNull(expected);
		assertEquals(user, expected);
	}

	@Test
	public void testFindById() {
		User user = setUpUserObject();
		insertUser(user);
		User actual = userDao.findById(user.getId(), User.class);
		assertEquals(user.getId(), actual.getId());
	}

	@Test
	public void testDelete() {
		User user = setUpUserObject();
		insertUser(user);
		userDao.delete(user.getId(), User.class);
		assertEquals(userDao.findById(user.getId(), User.class), null);
	}

	@Test
	public void testUpdate() {
		User user = setUpUserObject();
		user.setName("Original Name");
		insertUser(user);
		user.setName("Modified Name");
		userDao.update(user);
		assertEquals(userDao.findById(user.getId(), User.class).getName(), user.getName());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFindAll() {
		List<User> actual = userDao.findAll(User.class);
		List<User> expected = em.createQuery("FROM User").getResultList();
		assertEquals(expected.size(), actual.size());
	}
	
	private void insertUser(User user) {
		em.persist(user);
		em.flush();
		em.refresh(user);
	}
}
