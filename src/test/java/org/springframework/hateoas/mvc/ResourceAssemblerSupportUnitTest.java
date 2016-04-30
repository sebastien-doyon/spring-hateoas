package org.springframework.hateoas.mvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.LinkBuilderFactory;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ResourceAssemblerSupportUnitTest.class })
@WebAppConfiguration
@EnableWebMvc
// @RunWith(MockitoJUnitRunner.class)
public class ResourceAssemblerSupportUnitTest {
	@Mock
	LinkBuilderFactory<LinkBuilder> linkBuilderFactory;

	private MockMvc mockMvc;

	static class TestResourceAssemblerSupport extends ResourceAssemblerSupport<TestEntity, ResourceSupport> {

		public TestResourceAssemblerSupport(Class<?> controllerClass, Class<ResourceSupport> resourceType) {
			super(controllerClass, resourceType);
		}

		@Override
		public ResourceSupport toResource(TestEntity entity) {
			return null;
		}
	};

	static class TestEntity {
		private String value = "test";

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Controller
	static class FakeController {
		TestResourceAssemblerSupport assembler = new TestResourceAssemblerSupport(FakeController.class,
				ResourceSupport.class);

		@RequestMapping(value = "/api/test", method = RequestMethod.GET)
		@ResponseBody
		public List<ResourceSupport> findAll() {
			List<ResourceSupport> entities = new ArrayList<ResourceSupport>();
			entities.add(assembler.createResourceWithId(1, new TestEntity()));
			entities.add(assembler.createResourceWithId(2, new TestEntity()));
			entities.add(assembler.createResourceWithId(3, new TestEntity()));
			entities.add(assembler.createResourceWithId(4, new TestEntity()));
			entities.add(assembler.createResourceWithId(5, new TestEntity()));
			entities.add(assembler.createResourceWithId(6, new TestEntity()));

			return entities;
		}
	}

	@Before
	public void setUp() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();

		viewResolver.setViewClass(JstlView.class);
		viewResolver.setPrefix("/WEB-INF/jsp/");
		viewResolver.setSuffix(".jsp");

		mockMvc = MockMvcBuilders.standaloneSetup(new FakeController()).setViewResolvers(viewResolver).build();
	}

	@Test
	public void test_less_calls_to_http_request() throws Exception {
		boolean mock = false;
		SpyableMockHttpServletRequestBuilder req = new SpyableMockHttpServletRequestBuilder(mock, HttpMethod.GET, "/api/test")
				.contentType(new MediaType(MediaType.APPLICATION_JSON.getType(),
						MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8")));

		ResultActions actions = mockMvc.perform(req).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print());

		if (mock) 
		actions.andDo(new ResultHandler() {

			@Override
			public void handle(MvcResult result) throws Exception {
				MockHttpServletRequest req = result.getRequest();

				Mockito.verify(req, Mockito.times(1)).getServerName();
				Mockito.verify(req, Mockito.times(6)).getContextPath();
			}
		});
	}
}
