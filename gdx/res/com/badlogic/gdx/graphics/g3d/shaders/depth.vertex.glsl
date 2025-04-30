attribute vec3 a_position;
attribute vec4 a_color;  // Цвет вершины (RGBA в packed float формате)

uniform mat4 u_projViewWorldTrans;  // Матрица проекции

varying vec4 v_color;  // Передаем цвет во фрагментный шейдер

void main() {
    // Преобразуем позицию с помощью матрицы проекции
    gl_Position = u_projViewWorldTrans * vec4(a_position, 1.0);

    // Передаем цвет без изменений
    v_color = a_color;
}