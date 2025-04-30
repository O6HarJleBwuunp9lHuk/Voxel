#ifdef GL_ES
precision mediump float;  // Средняя точность для мобильных устройств
#endif

varying vec4 v_color;  // Получаем цвет из вершинного шейдера



void main() {
    // Просто используем переданный цвет
    gl_FragColor = v_color;
}