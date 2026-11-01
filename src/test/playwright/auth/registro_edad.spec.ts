import { test, expect } from '@playwright/test';

test.describe('Validación de edad - Ley 21.719 (P1.1.5)', () => {

  test('Debe mostrar error si el usuario tiene menos de 16 años', async ({ page }) => {
    // 1. Navegar al formulario de registro
    await page.goto('/registro');
    await page.waitForSelector('#nombreCompleto');

    // 2. Llenar formulario con fecha que da < 16 años
    await page.fill('#nombreCompleto', 'Menor de Edad');
    await page.fill('#email', 'menor@test.com');
    await page.fill('#fechaNacimiento', '2011-11-23'); // 14 años al 2026-07-19
    await page.fill('#password', 'Password123456!');
    await page.fill('#confirmarPassword', 'Password123456!');

    // 3. Hacer clic en Crear Cuenta
    await page.click('button[type="submit"]');

    // 4. Esperar a que la respuesta se renderice en el DOM (no redirección, no JSON)
    await page.waitForSelector('#form-response', { timeout: 5000 });

    // 5. Verificar que el mensaje de error aparece en el HTML
    const errorText = await page.textContent('#form-response');
    expect(errorText).toContain('16');
    expect(errorText).toContain('años');
  });

  test('Debe permitir registro si el usuario tiene 16 años o más', async ({ page }) => {
    await page.goto('/registro');
    await page.waitForSelector('#nombreCompleto');

    await page.fill('#nombreCompleto', 'Mayor de Edad');
    await page.fill('#email', `mayor-${Date.now()}@test.com`); // email único
    await page.fill('#fechaNacimiento', '1990-01-15'); // 36 años
    await page.fill('#password', 'Password123456!');
    await page.fill('#confirmarPassword', 'Password123456!');

    await page.click('button[type="submit"]');

    // Debe redirigir a /consentimiento (éxito)
    await page.waitForURL('**/consentimiento', { timeout: 5000 });
  });

  test('Debe permitir registro sin fecha de nacimiento (opcional)', async ({ page }) => {
    await page.goto('/registro');
    await page.waitForSelector('#nombreCompleto');

    await page.fill('#nombreCompleto', 'Sin Fecha');
    await page.fill('#email', `sinfecha-${Date.now()}@test.com`);
    // No llenar fechaNacimiento
    await page.fill('#password', 'Password123456!');
    await page.fill('#confirmarPassword', 'Password123456!');

    await page.click('button[type="submit"]');

    await page.waitForURL('**/consentimiento', { timeout: 5000 });
  });
});
