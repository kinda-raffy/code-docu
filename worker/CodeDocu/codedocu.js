import { Configuration, OpenAIApi } from "openai";
import fetchAdapter from "@haverstack/axios-fetch-adapter";
import runtime from "regenerator-runtime/runtime.js";


export default {
  async fetch(request, env) {
    return await handleRequest(request, env).catch(
      (err) => new Response(err.stack, { status: 500 })
    )
  },
};

export async function callDavinci(comment, env, temp) {
  const configuration = new Configuration({
    apiKey: env.OPENAI_API_KEY,
    baseOptions: {
      adapter: fetchAdapter
    }
  });
  const openai = new OpenAIApi(configuration);

  var prompt = `Make this sound more sophisticated (Don't include speech marks in the answer): ${comment}\nAnswer:`
  const { data } = await openai.createCompletion({
    model: "text-davinci-003",
    prompt: prompt,
    temperature: temp,
    max_tokens: 60,
    top_p: 1,
    frequency_penalty: 0.5,
    presence_penalty: 0,
  });

  // Delete unecessary params.
  delete data.id;
  delete data.created;
  delete data.object;
  delete data.model;

  const response = new Response(JSON.stringify(data), {
    headers: { 'Content-Type': 'application/json' },
    status: 200,
  });
  return response;
}

export async function handleRequest(request, env) {
  const { pathname } = new URL(request.url);
  
  // CodeDocu API.
  if (pathname.startsWith("/api")) {
    const comment = request.headers.get("Comment");
    if (!comment) {
      return new Response("No query given.", { status: 404 });
    }
    var temp = request.headers.get("Temperature");
    if (!temp) {
      temp = 0.7
    }
    temp = parseFloat(temp);
    return await callDavinci(comment, env, temp);
  }

  // No Endpoint Given.
  return new Response("No valid API endpoint queried.\nCodeDocu by @kindaraffy.", { status: 404 });
}
