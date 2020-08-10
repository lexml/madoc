export class Confirm {
    title  = 'Mensagem';
    text = '';
    buttons: string[] = ['OK', 'Cancelar'];
    callback: (button: string) => void;
}
