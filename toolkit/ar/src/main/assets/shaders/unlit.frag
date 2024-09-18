#version 300 es
/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
precision mediump float;


// The albedo and roughness/metallic textures.
uniform sampler2D u_AlbedoTexture;

in vec2 v_TexCoord;

layout(location = 0) out vec4 o_FragColor;

void main() {
  // Mirror texture coordinates over the X axis
  vec2 texCoord = vec2(v_TexCoord.x, 1.0 - v_TexCoord.y);

  // Skip all lighting calculations if the estimation is not valid.
  o_FragColor = vec4(texture(u_AlbedoTexture, texCoord).rgb, 1.0);
}
