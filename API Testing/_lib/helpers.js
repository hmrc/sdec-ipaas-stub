const FIRST_NAMES = ["John", "Sarah", "Jane", "David"];

const SURNAMES = ["Smith", "Doe", "Murphy", "Jones"];

const selectRandomElement = (arr) => {
    return arr[Math.floor(Math.random() * arr.length)];
};

const randomFirstName = () => selectRandomElement(FIRST_NAMES);

const randomSurname = () => selectRandomElement(SURNAMES);

const emailFor = (firstName, surname) =>
    `${firstName}.${surname}`.toLowerCase() + "@example.com";

module.exports = {
    randomFirstName,
    randomSurname,
    emailFor
};
