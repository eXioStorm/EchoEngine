// Graceful, accessible dropdown menu
(function(){
const button = document.getElementById('menuButton');
const menu = document.getElementById('menuList');
const menuItems = Array.from(menu.querySelectorAll('a'));


function isOpen(){
return button.getAttribute('aria-expanded') === 'true';
}


function openMenu(){
button.setAttribute('aria-expanded','true');
menu.classList.add('show');
// focus first item
setTimeout(()=> menuItems[0].focus(), 10);
}


function closeMenu(){
button.setAttribute('aria-expanded','false');
menu.classList.remove('show');
button.focus();
}


// Toggle on click
button.addEventListener('click', (e)=>{
e.stopPropagation();
if(isOpen()) closeMenu(); else openMenu();
});


// Close when clicking outside
document.addEventListener('click', (e)=>{
if(!menu.contains(e.target) && !button.contains(e.target)){
if(isOpen()) closeMenu();
}
});


// Keyboard support
document.addEventListener('keydown', (e)=>{
if(!isOpen()) return;


const currentIndex = menuItems.indexOf(document.activeElement);


switch(e.key){
case 'Escape':
closeMenu();
break;
case 'ArrowDown':
e.preventDefault();
const next = (currentIndex + 1) % menuItems.length;
menuItems[next].focus();
break;
case 'ArrowUp':
e.preventDefault();
const prev = (currentIndex - 1 + menuItems.length) % menuItems.length;
menuItems[prev].focus();
break;
case 'Tab':
// let Tab behave normally but close if focus leaves the menu
setTimeout(()=>{
if(!menu.contains(document.activeElement)) closeMenu();
}, 0);
break;
default:
break;
}
});


// If any menu link is clicked, close the menu (allows navigation)
menuItems.forEach(a => a.addEventListener('click', ()=> closeMenu()));


// Support opening the menu with ArrowDown from the button
button.addEventListener('keydown', (e)=>{
if(e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
e.preventDefault();
if(!isOpen()) openMenu();
}
});
})();